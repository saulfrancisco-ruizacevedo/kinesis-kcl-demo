# Kinesis KCL Demo

A demonstration of an AWS Kinesis Client Library (KCL) consumer written in Java.
It supports both local testing using LocalStack and production deployment using Docker Compose.

## 🚀 Features

* Java KCL consumer with Dockerized setup.
* Local environment simulated with LocalStack.
* Commands automated via Makefile.
* Multi-replica testing support (up to 3 concurrent consumers).
* Includes scripts to send test records and inspect DynamoDB checkpoints.

## 🧰 Requirements

* Docker and Docker Compose
* GNU Make
* Java 21+ (for building the application)
* Gradle (optional; `./gradlew` wrapper included)
* `jq` (optional, for parsing JSON in `make status`)

## 🏗️ Project Structure
```
.
├── Dockerfile
├── Makefile
├── build.gradle
├── docker-compose.yml
├── docker-compose.local.yml
├── localstack-init.sh
└── src/
```
---
## 🧭 Local Development

#### 🔹 Start everything (recommended)

This command builds the app, gives execution permission to `localstack-init.sh`,
and starts the environment (LocalStack + consumer):
``` bash
make up
```

#### 🔹 Build & start (force rebuild)
``` bash
make up-b
```

#### 🔹 Stop and remove all services
``` bash
make down
```

#### 🔹 Scale consumers (3 replicas)

Simulates multiple consumers reading from the same stream:

``` bash
make up-r
```

#### 🔹 Initialize LocalStack resources

If not automatically run, ensure your local Kinesis stream exists:

./localstack-init.sh

#### 🔹 Send test messages

Sends 10 messages in parallel to the Kinesis stream (`my-test-stream`):
``` bash
make send
```
#### 🔹 Check KCL checkpoint status

Inspects the DynamoDB table where KCL stores shard leases and checkpoints:
``` bash
make status
```

#### 🔹 Simulate consumer failure and recovery

Stops one consumer, waits 1 minute, and restarts it:
``` bash
make error
```
---
## 🌐 AWS Deployment

To run the application connected to a real AWS Kinesis Data Stream,
use the production compose configuration:
``` bash
docker-compose up -d
```

To tear it down:
``` bash
docker-compose down
```

💡 **Before deploying, make sure:**

* You have valid AWS credentials (via environment or IAM role).
* The Kinesis stream and DynamoDB lease table exist.
* Environment variables are correctly configured (stream name, region, etc.).
---
## ⚙️ Configuration

You can adjust key parameters in the compose files or Dockerfile:

* KCL app name (used for lease table name)
* Stream name
* AWS region
* LocalStack endpoint (for local mode)
* Number of replicas (via `make up-r`)

## 🧠 How It Works

The consumer uses AWS KCL to:

1.  Discover and manage Kinesis stream shards
2.  Balance work across multiple consumer instances
3.  Track checkpoints in DynamoDB
4.  Handle shard splits/merges and worker failover automatically

In local mode, LocalStack emulates Kinesis and DynamoDB APIs,
allowing full offline testing.

## 🧩 Build Commands

Build Java application:
``` bash
./gradlew clean build
```

Build Docker image:
``` bash
docker build -t kcl-demo-app .
```
---
## ✅ Verification

You should see logs confirming that the consumer:

* Connects to the `my-test-stream`
* Receives and processes messages
* Maintains shard checkpoints in DynamoDB

---
## 👨‍💻 Author
**Saul Francisco Ruiz Acevedo**  
GitHub: [@saulfrancisco-ruizacevedo](https://github.com/saulfrancisco-ruizacevedo)
