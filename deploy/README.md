# Deploy `agency-api` (ECR → ECS Fargate)

`.github/workflows/deploy-api.yml` ทำ 4 อย่าง:

1. `./mvnw test` (job `test`)
2. build image ด้วย buildx → push ขึ้น **ECR** (tag = commit SHA + `latest`)
3. render `deploy/task-definition.json` (ใส่ image, env, secrets, region, cpuArch) → `register-task-definition`
4. `update-service` แล้ว `aws ecs wait services-stable`

ไม่มี secret ถูก bake ลง image — ค่าถูกใส่ตอน register task definition (revision ใหม่ทุกครั้ง)

## Trigger

| event | environment |
|---|---|
| push `main` | `production` |
| push `develop` | `staging` |
| `workflow_dispatch` | เลือกเอง (staging / production) |

`environment` ที่เลือกจะกำหนดว่า GitHub หยิบ **Variables/Secrets ชุดไหน** มาใช้ (ต้องสร้าง Environment ใน repo settings ก่อน)

## Variables (ไม่ลับ) — ตั้งที่ Environment หรือ repo

| name | ตัวอย่าง | หมายเหตุ |
|---|---|---|
| `AWS_REGION` | `ap-southeast-1` | |
| `ECR_REPOSITORY` | `agency/agency-api` | **ชื่อ repo เท่านั้น** (ไม่ต้องใส่ registry/URI) — workflow derive `<account>.dkr.ecr.<region>.amazonaws.com` เอง |
| `ECS_CLUSTER` | `agency-prod` | ชื่อ cluster ที่มีอยู่จริง (`aws ecs describe-clusters --clusters <name>`) |
| `ECS_SERVICE` | `agency-api` | **ต้องมี service อยู่แล้ว** (workflow ไม่สร้าง) |
| `ECS_EXECUTION_ROLE_ARN` | `arn:aws:iam::123:role/agency-api-execution` | **IAM role ARN** (ไม่ใช่ ECS service ARN) ใช้ดึง image + เขียน CloudWatch |
| `ECS_TASK_FAMILY` | `agency-api` | ไม่ใส่ = `agency-api` |
| `DOCKER_PLATFORM` | `linux/arm64` (default) | ต้องตรงกับ `runtimePlatform` — workflow map ให้อัตโนมัติ (`arm64` → `ARM64`, อื่น ๆ → `X86_64`) และเปิด QEMU ให้เมื่อ build arm64 บน runner x86 |
| `APP_URL` | `https://dashboard.example.com` | ใช้โชว์ใน GitHub UI (optional) |

## Secrets (ลับ)

| name | จำเป็น | ใช้ทำอะไร |
|---|---|---|
| `AWS_ROLE_ARN` | ✅ (ถ้าใช้ OIDC) | role ที่ workflow assume — วิธีที่แนะนำ |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | ทางเลือก | fallback ถ้าองค์กรยังไม่ทำ OIDC (ถ้าตั้ง `AWS_ROLE_ARN` ด้วย OIDC จะชนะ) |
| `APP_ENV_JSON` | ไม่ | env เพิ่ม/ทับใน task definition เช่น `{"EXTERNAL_API_KEY":"..."}` หรือ array `[{"name":"X","value":"y"}]` |
| `APP_SECRETS_JSON` | ไม่ | **แนะนำสำหรับ secret จริง**: array ของ `{"name":"DB_PASSWORD","valueFrom":"arn:aws:secretsmanager:..."}` → ECS ฉีดให้ตอน start |

### เลือกแบบไหนสำหรับ secret

| วิธี | ค่า secret อยู่ที่ | มองเห็นได้จาก |
|---|---|---|
| `APP_SECRETS_JSON` + `valueFrom` (แนะนำ) | **AWS Secrets Manager / SSM** | คนที่มีสิทธิ์อ่าน secret นั้น |
| `APP_ENV_JSON` ใส่ค่าตรง ๆ | GitHub Secrets → task definition (plain env) | ใครก็ตามที่ `ecs:DescribeTaskDefinition` |

`secrets` ใน task definition ยังถูก inject เป็น env var ใน container — อย่า log ค่าและอย่าเปิด `actuator/env`

## IAM ที่ต้องเตรียม (ครั้งเดียว)

1. **GitHub OIDC provider** ใน AWS + role สำหรับ deploy ที่ trust เฉพาะ repo/env นี้:
```json
{
  "Effect": "Allow",
  "Principal": { "Federated": "arn:aws:iam::<acct>:oidc-provider/token.actions.githubusercontent.com" },
  "Action": "sts:AssumeRoleWithWebIdentity",
  "Condition": {
    "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
    "StringLike": {
      "token.actions.githubusercontent.com:sub": "repo:<org>/<repo>:environment:production"
    }
  }
}
```
2. สิทธิ์ของ deploy role: `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:PutImage`, `ecs:RegisterTaskDefinition`, `ecs:UpdateService`, `ecs:DescribeServices`, `ecs:DescribeTaskDefinition`, `iam:PassRole` (เฉพาะ execution role)
3. **Execution role** (คนละตัวกับ deploy role): `ecr:GetAuthorizationToken`/pull, `logs:CreateLogGroup`/`CreateLogStream`/`PutLogEvents`, และ `secretsmanager:GetSecretValue` หรือ `ssm:GetParameters` เฉพาะ ARN ที่ใช้

## Task definition

`deploy/task-definition.json` เป็น template ที่ commit ไว้ (family, cpu/memory 256/1024, port 8080, health check, awslogs, `stopTimeout: 30`)
workflow จะแทนค่า: `family`, `executionRoleArn`, `image`, `runtimePlatform`, `awslogs-region` และ merge `environment`/`secrets`

> หมายเหตุ: `application.yml` ในเครื่อง dev ตั้ง `server.port: 8081` แต่ container/ALB/nginx คาด 8080
> template จึงส่ง `SERVER_PORT=8080` ให้ — ถ้าจะเปลี่ยนพอร์ตจริงต้องแก้ทั้ง Dockerfile, compose, nginx, ALB ด้วย

## สิ่งที่ต้องมีอยู่ก่อน (workflow ไม่สร้างให้)

- ECR repository
- ECS cluster + service (Fargate, `awsvpc`, 2 subnets, SG เปิด 8080 จาก ALB SG)
- ALB target group ชี้ `/api/v1/health` หรือ `/api/v1/dashboard` (matcher 200)
- CloudWatch log group `/ecs/agency-api` (หรือให้ `awslogs-create-group=true` สร้างให้ — ต้องมีสิทธิ์ `logs:CreateLogGroup`)

## Error ที่เคยเจอ (และสาเหตุ)

| error | สาเหตุ | แก้ |
|---|---|---|
| `RepositoryNotFoundException ... DescribeRepositories` | ใส่ full image URI ใน `ECR_REPOSITORY` | ใช้ชื่อ repo เท่านั้น (`agency/agency-api`) |
| `Service cannot be empty` | `ECS_CLUSTER`/`ECS_SERVICE` ว่าง | ตั้ง variable ให้ครบ |
| `Get "https://<region>.amazonaws.com/v2/": no such host` | registry host ผิดรูปแบบ | workflow แก้แล้ว (derive จาก account) — ต้องอัปเดต workflow เป็นเวอร์ชันล่าสุด |
| `role ... is invalid` ตอน register | `ECS_EXECUTION_ROLE_ARN` เป็น ARN ของ ECS service | ใช้ `arn:aws:iam::<acct>:role/<name>` |
| `Subnet ID must match subnet-[0-9a-f]+` | ใช้ placeholder ใน `create-service` | ใส่ subnet ID จริง (2 AZ) |

## Rollback

```bash
# ดู revision ก่อนหน้า
aws ecs describe-services --cluster <cluster> --services <service> \
  --query 'services[0].taskDefinition'
# ย้อนกลับ
aws ecs update-service --cluster <cluster> --service <service> \
  --task-definition <family>:<previous-revision>
```
image ทุก revision ยังอยู่ใน ECR (tag = commit SHA) จึง rollback ได้เสมอ
ถ้าอยากได้ tag `latest` ด้วย ให้เพิ่ม `--tag "${registry}/${ECR_REPOSITORY}:latest"` — แต่ระวังถ้า staging/production ใช้ ECR repo เดียวกันจะทับกัน

## หมายเหตุเรื่องความเร็ว

build `linux/arm64` บน runner x86 ต้องผ่าน QEMU → ช้ากว่า native หลายเท่า (Maven build ใน container)
ถ้าอยากเร็วขึ้น ใช้ GitHub-hosted ARM runner (`runs-on: ubuntu-24.04-arm`) แล้วลบ step QEMU ออก
หรือเปลี่ยน `DOCKER_PLATFORM` เป็น `linux/amd64` (workflow จะตั้ง `X86_64` ให้เอง)
