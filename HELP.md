# kafka

### 基础
1. 结构组成
    - Producer 生产者
    - Consumer 消费者
    - Broker 服务代理节点,负责收到消息存储到磁盘
    - ZK 负责集群中元数据管理,控制器的选举
2. 重要概念
    - Topic(主题)kafka消息以主题为单位进行归类 
    - Partition(分区)为Topic子逻辑,一个主题有多个分区,一个主题下分区消息不同,分区在存储层面可以看作一个可追加的Log,消息  
    在被追加到分区时,会为其增加一个特定的偏移量(offset) 
    - Offset(消息分区中的唯一标识),kafka用其来保证[分区]的有序
    - Replica(副本) 同一分区有不同副本,leader负责处理读写,follower只负责同步
    - HD(高水位) 标识一个特定的offset,在他之前的消息对消费者才是可见的,消费者某个时间内不一定能知道全部从生产者发送的消息  
    多副本下,副本最低的LEO即为HD,所以副本同步速度会影响消费速度(同步情况)
    - LEO 下一条消息写入的offset  
    - last Consumed Offset 消费者的消费位移,而消费者提交的消费位移还应该+1(标识下一次消费开始的偏移量)
3. docker安装
    - ZK
        ``` 
        docker pull wurstmeister/zookeeper
        docker run -d --name zookeeper -p 2181:2181 -v /etc/localtime:/etc/localtime wurstmeister/zookeeper
        ```    
    - kafka
        ``` 
        docker pull wurstmeister/kafka
        # -e KAFKA_BROKER_ID=0  在kafka集群中，每个kafka都有一个BROKER_ID来区分自己
        # -e KAFKA_ZOOKEEPER_CONNECT=192.168.155.56:2181/kafka 配置zookeeper管理kafka的路径192.168.155.56:2181/kafka
        # -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://192.168.155.56:9092  把kafka的地址端口注册给zookeeper
        # -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 配置kafka的监听端口
        # -v /etc/localtime:/etc/localtime 容器时间同步虚拟机的时间
        docker run -d --name kafka  -p 9092:9092 -e KAFKA_BROKER_ID=0 -e KAFKA_ZOOKEEPER_CONNECT=81.68.186.201:2181/kafka -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://81.68.186.201:9092 -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 -v /etc/localtime:/etc/localtime wurstmeister/kafka
        ```    
    - ZK查看是否注册上去
        ```
        docker exec -it zookeeper /bin/sh
        ./zkCli.sh
        cd bin/
        ls / 
        ```    
4. 命令
    1. 创建topic
        ``` 
        --zookeeper 指定了 Kafka 所连接的 ZooKeeper 服务地址
        --topic 指定了所要创建主题的名称
        --replication-factor 指定了副本因子
        --partitions 指定了分区个数
        --create 是创建主题的动作指令
        
        ./kafka-topics.sh --zookeeper 81.68.186.201:2181/kafka --create --topic topic-demo --replication-factor 1 --partitions 1
        ```
    2. 发送消息
        ``` 
        --broker-list 指定了连接的 Kafka 集群地址
        --topic 指定了发送消息时的主题
        ./kafka-console-producer.sh --broker-list localhost:9092 --topic topic-demo
        ``` 
    3. 接受消息
        ``` 
         --bootstrap-server 指定了连接的 Kafka 集群地址
         --topic 指定了消费者订阅的主题
        ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic topic-demo
        ```    
5. 客户端
    1. 生产者消息在通过 send() 方法发往 broker 的过程中，有可能需要经过拦截器（Interceptor）、序列化器（Serializer）和分区器（Partitioner）  
       的一系列作用之后才能被真正地发往broker。          
        - 拦截器一共有两种拦截器：生产者拦截器和消费者拦截器
            * 生产者拦截器的使用也很方便，主要是自定义实现 org.apache.kafka.clients.producer.ProducerInterceptor 接口
                1. kafka会在序列化和分区前调用onSend方法
                2. KafkaProducer 会在消息被应答之前或消息发送失败时调用onAcknowledgement方法
                3. close() 方法主要用于在关闭拦截器时执行一些资源的清理工作
            * 实现自定义拦截器需要在配置中注册map.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,MyProducerInterceptor.class.getName()+",
            "+..其余拦截器);
        - 默认序列化器为org.apache.kafka.common.serialization.StringSerializer,很单纯的把字符串转为byte[] 
        - 分区器如果入参默认带了分区就不执行,不然会计算一个int的分区号
    2. 客户端由两个线程协调,这两个线程分别为主线程和Sender(发送线程)
        - 在主线程中由 KafkaProducer 创建消息，然后通过可能的拦截器、序列化器和分区器的作用之后缓存到消息累加器（也称为消息收集器）中
            * 消息累加器分为多个消息包(ProducerBatch),每个消息包里包含多个消息,发送消息以消息包的形式发送,减少网络请求次数
        - Sender 线程负责从 消息累加器 中获取消息并将其发送到 Kafka 中
        - 消息在发送前需要创建一块内存来保存对应的消息,在客户端中,kafka专门BufferPool来存放,来对消息(byte[])进行存放(复用),而对
        消息大于BufferPool的消息并不会复用,所以适当的扩大BufferPool来提高复用
        ![客户端架构](image/客户端架构.png)       
        - Sender线程会从消息收集器中获取缓存消息,转化为<Node,Request>的形式,在往kafka发送前还会保存到InFlightRequests中
            * 其中 Node 表示 Kafka 集群的 broker 节点。对于网络连接来说，生产者客户端是与具体的 broker 节点建立的连接，也就是向具体的 broker 节点发送消息，
            而并不关心消息属于哪一个分区,但是需要知道目标分区的 leader 副本所在的 broker 节点的地址、端口等信息才能建立连接
            * Request包含List<消息包>,kafka各种协议的请求
            * InFlightRequests主要缓存已经发送但是没有收到响应的请求,他可以限制连接Node的个数,最大的缓存数,当超过最大缓存数后,不能向该连接
            发送请求,可以通过队列数量和最大缓存值判断节点负载较大或网络连接有问题 
        -元数据是指 Kafka 集群的元数据，这些元数据具体记录了集群中有哪些主题，这些主题有哪些分区，每个分区的 leader 副本分配在哪个节点上，
        follower 副本分配在哪些节点上，哪些副本在 AR、ISR 等集合中，集群中有哪些节点，控制器节点又是哪一个等信息
            * 元数据虽然由 Sender 线程负责更新，但是主线程也需要读取这些信息
    3. 重要参数  
        - Acks,这个参数用来指定分区中必须要有多少个副本收到这条消息，之后生产者才会认为这条消息是成功写入的,它涉及消息的可靠性和吞吐量之间的权衡
            * acks = 1。(折中)默认值即为1。生产者发送消息之后，只要分区的 leader 副本成功写入消息，那么它就会收到来自服务端的成功响应
            * acks = 0。(最大吞吐量)生产者发送消息之后不需要等待任何服务端的响应
            * acks = -1 或 acks = all。(最强可靠性)生产者在消息发送之后，需要等待 ISR 中的所有副本都成功写入消息之后才能够收到来自服务端的成功响应      
        - max.request.size 这个参数用来限制生产者客户端能发送的消息的最大值,默认值为1MB
        - retries retries 参数用来配置生产者重试的次数，默认值为0，即在发生异常的时候不进行任何重试动作
            * retry.backoff.ms 这个参数的默认值为100，它用来设定两次重试之间的时间间隔，避免无效的频繁重试
        - connections.max.idle.ms 这个参数用来指定在多久之后关闭闲置的连接，默认值是540000（ms），即9分钟   
6. 消费者
    1. 消费组:每个消费者都有一个对应的消费组。当消息发布到主题后，只会被投递给订阅它的每个消费组中的一个消费者
        - 如果所有的消费者都隶属于同一个消费组，那么所有的消息都会被均衡地投递给每一个消费者，即每条消息只会被一个消费者处理，这就相当于点对点模式的应用
        - 如果所有的消费者都隶属于不同的消费组，那么所有的消息都会被广播给所有的消费者，即每条消息会被所有的消费者处理，这就相当于发布/订阅模式的应用
    2. 一个消费者可以订阅一个或多个主题 
    3. Kafka 中的消费是基于拉模式的。消息的消费一般有两种模式：推模式和拉模式。
        - 推模式是服务端主动将消息推送给消费者
        - 拉模式是消费者主动向服务端发起请求来拉取消息
    4. 控制或关闭消费
        - paused() 方法来返回被暂停的分区集合
        - wakeup() 方法是 KafkaConsumer 中唯一可以从其他线程里安全调用的方法。调用 wakeup() 方法后可以退出 poll() 的逻辑，
        并抛出 WakeupException 的异常，我们也不需要处理 WakeupException 的异常，它只是一种跳出循环的方式。
        - resume() 恢复某些分区向客户端返回数据的操作
7. 位移
    1. 对于 Kafka 中的分区而言，它的每条消息都有唯一的 offset，用来表示消息在分区中对应的位置,对于消费者而言，它也有一个 offset 的概念，
    消费者使用 offset 来表示消费到分区中某个消息所在的位置。
        - 对于消息在分区中的位置，我们将 offset 称为“偏移量”
        - 对于消费者消费到的位置，将 offset 称为“位移”，有时候也会更明确地称之为“消费位移”     
    2. 新消费者客户端中，消费位移存储在 Kafka 内部的主题__consumer_offsets 中。这里把将消费位移存储起来（持久化）的动作称为“提交”，
    消费者在消费完消息之后需要执行消费位移的提交   
    3. 在默认的方式下，消费者每隔5秒会将拉取到的每个分区中最大的消息位移进行提交。自动位移提交的动作是在 poll() 方法的逻辑里完成的，
    在每次真正向服务端发起拉取请求之前会检查是否可以进行位移提交，如果可以，那么就会提交上一次轮询的位移
        - 如若使用本地缓存,消费速度赶不上拉去速度,当位移提交过后,消费者崩溃,则本地缓存丢失,消息也丢失
            * 第三方缓存,如redis
        - 拉取消息后,尚未消费完毕,自动提交,消费者崩溃,消息丢失
            * 手动提交
        - 消费者拉取一批数据,消费中,尚未自动提交,消费者崩溃,重启,重新拉数据,重复消费    
        - 消费者使用自动提交offset，但当还没有提交的时候，有新的消费者加入或者移除，发生了rebalance。再次消费的时候，消费者会根据提交的偏移量来，于是重复消费了数据
        - 消息处理耗时，或者消费者拉取的消息量太多，处理耗时，超过了max.poll.interval.ms的配置时间，导致认为当前消费者已经死掉，触发再均衡
            * 消息表
            * 数据库唯一索引
            * 缓存消费过的消息id
    4. kafka支持手动提交,步骤如下
        - props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);//关闭自动提交
        - commitSync()||commitAsync() 同步提交和异步提交
    5. 指定消费位移
        - 当一个新的消费组建立的时候，它根本没有可以查找的消费位移。或者消费组内的一个新消费者订阅了一个新的主题，它也没有可以查找的消费位移。
        当 __consumer_offsets 主题中有关这个消费组的位移信息过期而被删除后，它也没有可以查找的消费位移,会根据auto.offset.reset来决定从何处开始进行消费
            * auto.offset.reset(默认为latest:表示从分区末尾开始消费消息,earliest:那么消费者会从起始处，也就是0开始消费)  
        - KafkaConsumer.seek()
         