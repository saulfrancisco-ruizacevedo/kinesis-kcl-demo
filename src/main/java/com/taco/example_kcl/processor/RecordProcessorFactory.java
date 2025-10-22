package com.taco.example_kcl.processor;

import org.springframework.stereotype.Component;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

/**
 * Factory for creating RecordProcessor instances.
 */
@Component
public class RecordProcessorFactory implements ShardRecordProcessorFactory {

    @Override
    public ShardRecordProcessor shardRecordProcessor() {
        // Return a new instance of our processor logic
        return new RecordProcessor();
    }
}