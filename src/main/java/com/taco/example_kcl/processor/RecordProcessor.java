package com.taco.example_kcl.processor;

import lombok.extern.slf4j.Slf4j;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.nio.charset.StandardCharsets;

/**
 * The main class that implements the record processing logic.
 */
@Slf4j
public class RecordProcessor implements ShardRecordProcessor {

    private String shardId;

    @Override
    public void initialize(InitializationInput initializationInput) {
        this.shardId = initializationInput.shardId();
        log.info("Initializing RecordProcessor for Shard: {}", this.shardId);
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        log.info("Processing {} records from Shard: {}", processRecordsInput.records().size(), this.shardId);

        for (KinesisClientRecord record : processRecordsInput.records()) {
            try {
                String data = StandardCharsets.UTF_8.decode(record.data()).toString();
                log.info("[Shard: {}] Data: {}, Seq: {}",
                        this.shardId, data, record.sequenceNumber());

                // Simulate work
                Thread.sleep(50);

            } catch (Exception e) {
                log.error("Error processing record. It will be retried.", e);
            }
        }

        // Checkpoint after the batch is processed
        try {
            log.info("Checkpointing progress for Shard: {}", this.shardId);
            processRecordsInput.checkpointer().checkpoint();
        } catch (InvalidStateException | ShutdownException e) {
            log.error("Checkpoint failed. Records will be re-processed.", e);
        }
    }

    @Override
    public void leaseLost(LeaseLostInput leaseLostInput) {
        log.warn("Lease lost for Shard: {}", this.shardId);
    }

    @Override
    public void shardEnded(ShardEndedInput shardEndedInput) {
        log.info("Shard {} has ended. Final checkpoint.", this.shardId);
        try {
            shardEndedInput.checkpointer().checkpoint();
        } catch (InvalidStateException | ShutdownException e) {
            log.error("Final checkpoint failed on shardEnded", e);
        }
    }

    @Override
    public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
        log.info("Shutdown requested for Shard: {}. Final checkpoint.", this.shardId);
        try {
            shutdownRequestedInput.checkpointer().checkpoint();
        } catch (InvalidStateException | ShutdownException e) {
            log.error("Final checkpoint failed on shutdownRequested", e);
        }
    }
}