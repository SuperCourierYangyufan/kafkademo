package com.my.kafkademo.demo;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * @author 杨宇帆
 */
@Log4j2
public class KafkaConsumerAnalysis {
    public static final String brokerList = "81.68.186.201:9092";
    public static final String topic = "topic-demo";
    public static final String groupId = "group.demo";
    public static final AtomicBoolean isRunning = new AtomicBoolean(true);


    public static Properties initConfig(){
        Properties props = new Properties();
        //常用枚举值
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                  "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
                  StringDeserializer.class.getName());
        props.put("bootstrap.servers", brokerList);
        //消费者隶属的消费组的名称，默认值为“”。如果设置为空，则会报出异常
        props.put("group.id", groupId);
        //这个参数用来设定 KafkaConsumer 对应的客户端id，默认值也为“”。
        // 如果客户端不设置，则 KafkaConsumer 会自动生成一个非空字符串，
        // 内容形式如“consumer-1”、“consumer-2”，即字符串“consumer-”与数字的拼接。
        props.put("client.id", "test-consumer");
        return props;
    }

    public static void main(String[] args) {
        Properties props = initConfig();
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        //订阅主题,可多个
        //可模糊 可正则
        //consumer.subscribe(Pattern.compile("topic-.*"));
        consumer.subscribe(Arrays.asList(topic));
        //可以指定只消费特定分区
        //这里只订阅 topic-demo 主题中分区编号为0的分区
//        consumer.assign(Arrays.asList(new TopicPartition("topic-demo", 0)));
        //订阅全部分区
//        List<TopicPartition> partitions = new ArrayList<>();
//        List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
//        if (partitionInfos != null) {
//            for (PartitionInfo tpInfo : partitionInfos) {
//                partitions.add(new TopicPartition(tpInfo.topic(), tpInfo.partition()));
//            }
//        }
//        consumer.assign(partitions);
        try {
            while (isRunning.get()) {
                //对于 poll() 方法而言，如果某些分区中没有可供消费的消息，那么此分区对应的消息拉取的结果就为空；
                // 如果订阅的所有分区中都没有可供消费的消息，那么 poll() 方法返回为空的消息集合
                //注意到 poll() 方法里还有一个超时时间参数 timeout，用来控制 poll() 方法的阻塞时间(毫秒)，
                // 在消费者的缓冲区里没有可用数据时会发生阻塞
                ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records){
                //消费指定分区
//                for (ConsumerRecord<String, String> record : records.records(new TopicPartition(topic,0))) {
                    System.out.println("topic = " + record.topic()
                                           + ", partition = "+ record.partition()
                                           + ", offset = " + record.offset());
                    System.out.println("key = " + record.key()
                                           + ", value = " + record.value());
                    //do something to process record.
                }
            }
        } catch (Exception e) {
            log.error("occur exception ", e);
        } finally {
            consumer.close();
        }
    }
}
