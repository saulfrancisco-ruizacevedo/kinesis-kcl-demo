SERVICE_NAME=kcl-consumer

up: build
	chmod +x ./localstack-init.sh
	@echo "Starting services..."
	docker-compose -f docker-compose.local.yml up -d --wait

up-b: build
	@echo "Building & Starting services..."
	docker-compose -f docker-compose.local.yml up --build -d --wait

down:
	@echo "Stopping and removing services..."
	docker-compose -f docker-compose.local.yml down

up-r: build
	@echo "Starting 3 replicas of $(SERVICE_NAME)..."
	docker-compose -f docker-compose.local.yml up --scale $(SERVICE_NAME)=3 -d

send:
	@echo "Sending 10 test messages in PARALLEL to 'my-test-stream' via LocalStack..."
	docker-compose exec localstack bash -c ' \
		for i in {1..10}; do \
			( \
				echo "Sending message $$i/10..."; \
				awslocal kinesis put-record \
					--stream-name my-test-stream \
					--partition-key "$$(date +%s%N)" \
					--data "Hello KCL message #$$i"; \
			) & \
		done; \
		wait; \
		echo "All 10 parallel messages sent."; \
	'

status:
	@echo "Checking DynamoDB lease/checkpoint table 'my-kcl-consumer-app'..."
	docker-compose -f docker-compose.local.yml exec localstack awslocal dynamodb scan \
		--table-name my-kcl-consumer-app | \
		jq '.Items[] | {shard: .leaseKey.S, owner: .leaseOwner.S, checkpoint: .checkpoint.S}'

error:
	@echo "Simulating error on consumer-2 (example-kcl-kcl-consumer-2)..."
	docker stop example-kcl-kcl-consumer-2
	@echo "Waiting for 1 minute..."
	sleep 60
	@echo "Starting consumer-2 (example-kcl-kcl-consumer-2)..."
	docker start example-kcl-kcl-consumer-2