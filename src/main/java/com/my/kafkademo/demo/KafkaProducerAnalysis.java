package com.my.kafkademo.demo;

import java.util.Properties;
import java.util.Scanner;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * @author 杨宇帆
 */
@Log4j2
public class KafkaProducerAnalysis {
    public static final String brokerList = "81.68.186.201:9092";
    public static final String topic = "topic-demo";

    public static Properties initConfig(){
        Properties map = new Properties();
        //Kafka 集群所需的 broker 地址清单
        map.put("bootstrap.servers", brokerList);
        //序列化
        map.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        map.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        //这个参数用来设定 KafkaProducer 对应的客户端id，默认值为“”。
        // 如果客户端不设置，则 KafkaProducer 会自动生成一个非空字符串，
        // 内容形式如“producer-1”、“producer-2”，即字符串“producer-”与数字的拼接
        map.put("client.id", "test-producer");
        return map;
    }

    public static void main(String[] args) {
        Properties props = initConfig();
        KafkaProducer<String, String> producer = new KafkaProducer(props);
        Scanner scanner = new Scanner(System.in);
        try {
            while (true){
            ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, scanner.nextLine());
            producer.send(record) ;
            }
        }catch (Exception e){
            log.error("occur exception ", e);
        }finally {
            producer.close();
        }
    }
}
