package com.data_analysis;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.data_analysis.CustomerInfoRecord.Customer;
import com.data_analysis.CustomerInfoRecord.Customer.AddressType;
import com.data_analysis.CustomerInfoRecord.Customer.PhoneNumber;
import com.data_analysis.CustomerInfoRecord.Customer.PhoneType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Producer {
    public static JsonNode streamToNode(InputStream stream) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node;
        try {
            node = mapper.readTree(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return node;
    }
    
    public static List<Customer> transformJSONToProtoBuf(ArrayNode node) {
    	Map<String, Object> nodeMap = null;
    	List<Customer> customers = new ArrayList<Customer>();
    	
    	for (int i = 0; i< node.size(); i++) {
            JsonNode jsonNode = node.get(i);
            
            if (jsonNode instanceof ObjectNode) {
            	ObjectMapper mapper = new ObjectMapper();
            	nodeMap = mapper.convertValue(jsonNode, Map.class);
            	
            	Map<String, Object> customer = (Map<String, Object>)nodeMap.get("customer");
            	
            	Customer.Builder customerBuilder = Customer.newBuilder();
            	
            	customerBuilder.setName((String)customer.get("name"));
            	
            	String idStr = (String)customer.get("id");
            	customerBuilder.setId(Integer.valueOf(idStr));
            	
            	customerBuilder.setEmail((String)customer.get("email"));
            	
            	addPhoneNumber(customer, customerBuilder);
            	addAddress(customer, customerBuilder);
            	
            	customers.add(customerBuilder.build());
            }
    	}
    	
    	return customers;
    	
    }
    
    private static void addPhoneNumber(Map<String, Object> customer, Customer.Builder customerBuilder) {
    	List<Object> phones = (List)customer.get("phones");
    	
    	for (Object phoneObj : phones) {
			Map<String, Object> phone = (Map<String, Object>)phoneObj;
			PhoneNumber.Builder phoneBuilder = PhoneNumber.newBuilder();
			
			phoneBuilder.setNumber((String)phone.get("number"));
			String type = (String)phone.get("type");
			if(PhoneType.HOME.name().equals(type)) {
				phoneBuilder.setType(PhoneType.HOME);
			} else if(PhoneType.MOBILE.name().equals(type)) {
				phoneBuilder.setType(PhoneType.MOBILE);
			} else if(PhoneType.WORK.name().equals(type)) {
				phoneBuilder.setType(PhoneType.WORK);
			}
			
			customerBuilder.addPhones(phoneBuilder.build());
		}
    }
    
    private static void addAddress(Map<String, Object> customer, Customer.Builder customerBuilder) {
    	Map<String, Object> address = (Map<String, Object>)customer.get("address");
    	AddressType.Builder addressBuilder = AddressType.newBuilder();
    	
    	addressBuilder.setLine1((String)address.get("line1"));
    	addressBuilder.setLine2((String)address.get("line2"));
    	addressBuilder.setLine3((String)address.get("line3"));
    	
    	addressBuilder.setCity((String)address.get("city"));
    	addressBuilder.setZipCode((String)address.get("zipCode"));
    	addressBuilder.setState((String)address.get("state"));
    	
    	customerBuilder.setAddress(addressBuilder.build());
    }
    
    public static void produceProtoBufMessagesToKafka(List<Customer> customersAsProtoBufs) throws InterruptedException, ExecutionException {
    	Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        String topic = "data_analysis";
        
        org.apache.kafka.clients.producer.Producer<String, byte[]> producer = new KafkaProducer<String, byte[]>(props);

        for (int i = 0; i<customersAsProtoBufs.size(); ++i) {
        	Customer customer = customersAsProtoBufs.get(i);
        	byte[] byteArray = customer.toByteArray();
            ProducerRecord<String, byte[]> record = 
            		new ProducerRecord<String,byte[]>(topic, String.valueOf(customer.getId()), byteArray);
            producer.send(record).get();
        }

        producer.close();

        System.out.println("Done!");

    }
    
    public static void main(String[] args) throws InterruptedException, ExecutionException {
    	InputStream cirStream = Producer.class.getResourceAsStream("/CustomerInfoRecords.json");
    	JsonNode jsonNode = Producer.streamToNode(cirStream);
    	List<Customer> customersAsProtoBufs = Producer.transformJSONToProtoBuf((ArrayNode)jsonNode);
    	System.out.println("customersAsProtoBufs: " + customersAsProtoBufs);
    	produceProtoBufMessagesToKafka(customersAsProtoBufs);
    }
}
