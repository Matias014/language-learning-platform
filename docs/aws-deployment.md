# AWS Deployment Notes

This document describes the initial AWS deployment approach for the Language Learning Platform.

The first AWS deployment target is a simple, cost-conscious setup based on Amazon EC2 and Docker Compose.

## Initial deployment target

```text
Amazon EC2
Docker Compose
Angular frontend served by Nginx
Spring Boot backend
PostgreSQL running as a Docker container
IAM role for EC2
CloudWatch for monitoring and billing alerts
```

This setup is intended for portfolio and recruitment purposes. It demonstrates practical usage of AWS without introducing unnecessary infrastructure complexity or avoidable costs at the first deployment stage.

Amazon RDS PostgreSQL and Amazon S3 are documented as possible future improvements, not as required services for the initial deployment.

## Architecture

```text
User browser
    |
    | HTTP :80
    v
Amazon EC2
    |
    | Docker Compose
    |
    |-- frontend container
    |   Angular production build served by Nginx
    |   Public port: 80
    |
    |-- backend container
    |   Spring Boot REST API
    |   Internal port: 8080
    |
    |-- database container
        PostgreSQL
        Internal port: 5432
```

The frontend is the only service exposed publicly.

The backend and database are not exposed directly to the internet. The frontend Nginx container proxies `/api` requests to the backend container inside the Docker network.

## AWS services used

### Amazon EC2

Amazon EC2 is used to run the application on a Linux virtual machine.

The EC2 instance runs:

- Docker
- Docker Compose
- Angular frontend container
- Spring Boot backend container
- PostgreSQL database container

This is the first practical AWS deployment target for the project.

### IAM

The EC2 instance should use an IAM role instead of static AWS access keys.

The initial role can be minimal. If CloudWatch Agent is used, the EC2 role should include permissions such as:

```text
CloudWatchAgentServerPolicy
```

Using an IAM role avoids storing long-term AWS credentials on the server or in the repository.

### Amazon CloudWatch

CloudWatch is used for monitoring and cost safety.

At the initial stage, the AWS account should have:

- AWS Budget configured
- CloudWatch billing alarm configured
- optional CloudWatch Agent for EC2/container logs

The billing alarm is not a hard spending limit. It only sends notifications. Cost control still requires stopping or terminating unused AWS resources.

### PostgreSQL

The initial deployment runs PostgreSQL as a Docker container on the EC2 instance.

This avoids the additional cost and complexity of Amazon RDS at the first deployment stage.

PostgreSQL data is stored in a Docker volume:

```text
database_data
```

### Amazon RDS PostgreSQL

Amazon RDS PostgreSQL is a planned future improvement.

RDS can later replace the PostgreSQL Docker container.

In that version, the backend would connect to RDS through environment variables:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

The RDS instance should not be publicly accessible. Access to port `5432` should be allowed only from the EC2 security group.

### Amazon S3

Amazon S3 is also a possible future improvement.

It can be used for persistent storage of user-generated files, for example:

- user avatars
- achievement icons
- exported files
- other uploaded assets

The current initial EC2 deployment uses local Docker volume storage for uploaded files.

## Deployment files

The initial EC2 deployment uses:

```text
compose.ec2.yaml
.env.ec2
.env.ec2.example
```

Only the example environment file should be committed:

```text
.env.ec2.example
```

The real environment file must not be committed:

```text
.env.ec2
```

## Example `.env.ec2.example`

```env
POSTGRES_PASSWORD=replace-with-postgres-password
SECURITY_JWT_SECRET=replace-with-base64-32-byte-secret
SECURITY_FIELD_ENCRYPTION_KEY=replace-with-base64-32-byte-secret
ANTHROPIC_API_KEY=
```

## Example `compose.ec2.yaml`

```yaml
name: language-learning-platform

services:
  database:
    image: postgres:17-alpine
    container_name: language-learning-database
    environment:
      POSTGRES_DB: language_application
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - database_data:/var/lib/postgresql/data
      - ./backend/docker/initdb:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d language_application"]
      interval: 5s
      timeout: 5s
      retries: 20
    restart: unless-stopped

  backend:
    container_name: language-learning-backend
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://database:5432/language_application
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SECURITY_JWT_SECRET: ${SECURITY_JWT_SECRET}
      SECURITY_FIELD_ENCRYPTION_KEY: ${SECURITY_FIELD_ENCRYPTION_KEY}
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
    depends_on:
      database:
        condition: service_healthy
    volumes:
      - backend_avatars:/app/photos/avatars
    restart: unless-stopped

  frontend:
    container_name: language-learning-frontend
    build:
      context: ./frontend
      dockerfile: Dockerfile
    depends_on:
      - backend
    ports:
      - "80:80"
    restart: unless-stopped

volumes:
  database_data:
  backend_avatars:
```

## Local verification

Before deploying to AWS, the EC2 Compose file can be tested locally.

Create a local environment file:

```bash
cp .env.ec2.example .env.ec2
```

Generate secrets:

```bash
openssl rand -base64 32
openssl rand -base64 32
```

Example local `.env.ec2`:

```env
POSTGRES_PASSWORD=postgres
SECURITY_JWT_SECRET=replace-with-generated-base64-secret
SECURITY_FIELD_ENCRYPTION_KEY=replace-with-generated-base64-secret
ANTHROPIC_API_KEY=
```

Start the application:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml up --build
```

Open:

```text
http://localhost
```

If local port `80` is already in use, temporarily change the frontend port mapping in `compose.ec2.yaml`:

```yaml
ports:
  - "8088:80"
```

Then open:

```text
http://localhost:8088
```

For AWS EC2 deployment, keep:

```yaml
ports:
  - "80:80"
```

## AWS account safety setup

Before creating EC2 resources, configure cost protection:

- AWS Budget
- Zero spend budget
- Monthly safety budget
- CloudWatch billing alarm
- email notifications

Recommended thresholds:

```text
Budget: 5 USD monthly
CloudWatch billing alarm: 1 USD
```

These alerts do not stop AWS services automatically. They only notify about estimated or actual costs.

## Recommended AWS region

Recommended region for the EC2 deployment:

```text
Europe (Frankfurt) / eu-central-1
```

Billing alarms are configured separately in:

```text
US East (N. Virginia) / us-east-1
```

## IAM role for EC2

Create an IAM role for the EC2 instance.

Recommended role name:

```text
language-learning-ec2-role
```

Trusted entity:

```text
AWS service
EC2
```

Optional policy for CloudWatch Agent:

```text
CloudWatchAgentServerPolicy
```

The role should be attached to the EC2 instance during instance creation.

## EC2 security group

Recommended inbound rules:

```text
HTTP  80   0.0.0.0/0
SSH   22   your-ip-address-only
```

Do not expose these ports publicly:

```text
8080
5432
```

The backend should stay internal inside the Docker network.

The PostgreSQL database should stay internal inside the Docker network.

## Create EC2 instance

Recommended instance configuration:

```text
Name: language-learning-platform
AMI: Ubuntu Server 24.04 LTS or Ubuntu Server 22.04 LTS
Instance type: t3.micro or t2.micro
Key pair: new .pem key pair
IAM instance profile: language-learning-ec2-role
Storage: 8-20 GB gp3
```

Download the `.pem` key and keep it private.

Do not commit `.pem` files to Git.

## Connect to EC2

On the local machine:

```bash
chmod 400 language-learning-key.pem
ssh -i language-learning-key.pem ubuntu@EC2_PUBLIC_IP
```

For Ubuntu AMIs, the default SSH user is usually:

```text
ubuntu
```

## Install Docker on EC2

Run on the EC2 instance:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo tee /etc/apt/keyrings/docker.asc > /dev/null
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo usermod -aG docker $USER
```

Log out and log in again:

```bash
exit
```

Reconnect:

```bash
ssh -i language-learning-key.pem ubuntu@EC2_PUBLIC_IP
```

Verify installation:

```bash
docker --version
docker compose version
git --version
```

## Deploy application on EC2

Clone the repository:

```bash
git clone https://github.com/Matias014/language-learning-platform.git
cd language-learning-platform
```

Create the environment file:

```bash
cp .env.ec2.example .env.ec2
nano .env.ec2
```

Generate secrets:

```bash
openssl rand -base64 32
openssl rand -base64 32
```

Fill `.env.ec2`:

```env
POSTGRES_PASSWORD=replace-with-strong-postgres-password
SECURITY_JWT_SECRET=replace-with-generated-base64-secret
SECURITY_FIELD_ENCRYPTION_KEY=replace-with-generated-base64-secret
ANTHROPIC_API_KEY=
```

Start the application:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml up -d --build
```

Check containers:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml ps
```

Check logs:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml logs -f
```

Backend logs only:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml logs -f backend
```

Frontend logs only:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml logs -f frontend
```

Database logs only:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml logs -f database
```

## Application URL

After deployment, the application should be available at:

```text
http://EC2_PUBLIC_IP
```

The backend is reached through the frontend Nginx proxy under:

```text
/api
```

The backend direct port `8080` should not be publicly exposed.

## Default local credentials

The local development seed may create an administrator account:

```text
login: admin
password: password
```

These credentials are intended only for local testing and portfolio demo usage.

## Updating the application

On EC2:

```bash
cd language-learning-platform
git pull
docker compose --env-file .env.ec2 -f compose.ec2.yaml up -d --build
```

## Stopping the application

Stop containers:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml down
```

Stop containers and remove Docker volumes:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml down -v
```

Removing volumes deletes the local PostgreSQL data stored in Docker.

## Stopping EC2 to reduce costs

If the deployment is not needed continuously, stop the EC2 instance from the AWS console:

```text
EC2
Instances
Select instance
Instance state
Stop instance
```

If the deployment is no longer needed, terminate the instance:

```text
EC2
Instances
Select instance
Instance state
Terminate instance
```

Stopping or terminating unused resources is the main way to avoid unnecessary AWS charges.

## Optional CloudWatch Agent setup

CloudWatch Agent can be added later to collect system and application logs.

Possible log sources:

```text
Docker container logs
EC2 system logs
Application startup logs
Backend error logs
```

This is useful for demonstrating basic AWS monitoring knowledge.

## Optional RDS improvement

A future improvement is to replace the PostgreSQL Docker container with Amazon RDS PostgreSQL.

The future architecture would be:

```text
Amazon EC2
Docker Compose
Frontend container
Backend container
    |
    | JDBC
    v
Amazon RDS PostgreSQL
```

In that setup:

- the database container would be removed from `compose.ec2.yaml`
- the backend would use `SPRING_DATASOURCE_URL`
- RDS would be private
- RDS port `5432` would allow inbound traffic only from the EC2 security group

This is not required for the initial deployment.

## Optional S3 improvement

A future improvement is to move uploaded file storage from Docker volumes to Amazon S3.

Possible backend design:

```text
FileStorageService
    |
    |-- LocalFileStorageService
    |-- S3FileStorageService
```

The S3 implementation could store:

```text
avatars/
achievements/
exports/
```

Recommended AWS setup for S3:

- private S3 bucket
- IAM role assigned to EC2
- no static AWS access keys in the repository
- object access controlled by the backend

This is not required for the initial deployment.

## Production improvements

This setup is intended as a portfolio-oriented deployment.

Before production use, the following improvements should be added:

- HTTPS
- domain name
- stronger secret management with AWS Secrets Manager or Systems Manager Parameter Store
- private database setup
- database backups
- CloudWatch alarms
- health checks
- CI/CD pipeline
- separate production Spring profile
- proper migration tool such as Flyway or Liquibase

## Security checklist

- Do not commit `.env.ec2`
- Do not commit `.env.aws`
- Do not commit `.pem` SSH keys
- Do not commit AWS access keys
- Do not commit real API keys
- Do not expose PostgreSQL publicly
- Do not expose backend port `8080` publicly
- Restrict SSH to your own IP
- Use IAM roles instead of static AWS credentials
- Replace demo JWT secrets
- Replace demo field encryption secrets
- Use HTTPS for public deployments

## Portfolio summary

This deployment demonstrates practical usage of:

- Docker
- Docker Compose
- Amazon EC2
- IAM roles
- CloudWatch billing alerts
- environment-based configuration
- full-stack deployment of Angular and Spring Boot
- PostgreSQL container deployment

Possible future improvements include:

- Amazon RDS PostgreSQL
- Amazon S3 file storage
- CloudWatch Agent log collection
- HTTPS and domain configuration
