package com.data_analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.proto.ProtoWriteSupport;

import com.data_analysis.CustomerInfoRecord.Customer;

public class Consumer {
	
    /**
     * Converts Protobuf messages to Proto-Parquet and writes them in the specified path.
     */
    public static void writeToParquetFile(Path file,
                                          Collection<CustomerInfoRecord.Customer> customers) throws IOException {

        Configuration conf = new Configuration();
        ProtoWriteSupport.setWriteSpecsCompliant(conf, true);

        try (ParquetWriter<CustomerInfoRecord.Customer> writer = new ParquetWriter(
                                                file,
                                                new ProtoWriteSupport(CustomerInfoRecord.Customer.class)
                                                )) {
            for (CustomerInfoRecord.Customer record : customers) {
                writer.write(record);
            }
        }
    }
    
	public static void main(String[] args) throws ExecutionException, InterruptedException, IOException  {

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", UUID.randomUUID().toString());
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        String topic = "data_analysis";

        org.apache.kafka.clients.consumer.Consumer<String, byte[]> consumer = new KafkaConsumer<String, byte[]>(props);

        consumer.subscribe(Arrays.asList(topic));

        final int DEFAULT_BATCH_SIZE = 10;
        int batchSize = 0;
        
        Collection<CustomerInfoRecord.Customer> customers = new ArrayList<CustomerInfoRecord.Customer>();
        
        boolean done = false;
        
        while (!done) {
            ConsumerRecords<String, byte[]> records = consumer.poll(100);
            for (ConsumerRecord<String, byte[]> record : records) {
            	CustomerInfoRecord.Customer customer = Customer.parseFrom(record.value());
            	customers.add(customer);
                System.out.println("Customer: " + customer);
                batchSize++;
                if(batchSize == DEFAULT_BATCH_SIZE) {
                	done = true;
                }
            }
        }
        
        Path outputPath = new Path("/tmp/customers.parquet");
        writeToParquetFile(outputPath, customers);
    }
}
