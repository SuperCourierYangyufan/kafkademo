package com.my.kafkademo.demo;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * @author 杨宇帆
 */
public class KafkaProducerAnalysis {
    public static final String brokerList = "81.68.186.201:9092";
    public static final String topic = "topic-demo";

    public static Map<String,Object> initConfig(){
        Map map = new HashMap<String,String>();
        map.put("bootstrap.servers", brokerList);
        map.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        map.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        map.put("client.id", "producer.client.id.demo");
        return map;
    }

    public static void main(String[] args) {
        Map<String,Object> props = initConfig();
        KafkaProducer<String, String> producer = new KafkaProducer(props);
        ProducerRecord<String, String> record =
            new ProducerRecord<>(topic, "Hello, Kafka!");
        try {
            producer.send(record);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            producer.close();
        }
    }
}
