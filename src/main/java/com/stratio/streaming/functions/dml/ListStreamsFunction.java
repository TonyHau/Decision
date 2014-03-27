package com.stratio.streaming.functions.dml;

import java.util.List;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.apache.spark.api.java.JavaRDD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.stratio.streaming.commons.constants.BUS;
import com.stratio.streaming.commons.constants.STREAM_OPERATIONS;
import com.stratio.streaming.commons.messages.ColumnNameTypeValue;
import com.stratio.streaming.commons.messages.ListStreamsMessage;
import com.stratio.streaming.commons.messages.StratioStreamingMessage;
import com.stratio.streaming.functions.StratioStreamingBaseFunction;

public class ListStreamsFunction extends StratioStreamingBaseFunction {
	
	private static Logger logger = LoggerFactory.getLogger(ListStreamsFunction.class);
	private String kafkaCluster;
	private Producer<String, String> producer;

	/**
	 * 
	 */
	private static final long serialVersionUID = 7911766880059394316L;

	public ListStreamsFunction(SiddhiManager siddhiManager, String zookeeperCluster, String kafkaCluster) {
		super(siddhiManager, zookeeperCluster, kafkaCluster);
		this.kafkaCluster = kafkaCluster;
		this.producer = new Producer<String, String>(createProducerConfig());
	}
	

	@Override
	public Void call(JavaRDD<StratioStreamingMessage> rdd) throws Exception {
		
		List<StratioStreamingMessage> requests = rdd.collect();
		
		for (StratioStreamingMessage request : requests) {
			
			List<StratioStreamingMessage> streams = Lists.newArrayList();
			
			for(StreamDefinition streamMetaData : getSiddhiManager().getStreamDefinitions()) {
				
				List<ColumnNameTypeValue> columns = Lists.newArrayList();
				
				for (Attribute column : streamMetaData.getAttributeList()) {
					columns.add(new ColumnNameTypeValue(column.getName(), column.getType().toString(), null));
				}
				
				streams.add(new StratioStreamingMessage(streamMetaData.getId(), columns));
			}
			
			KeyedMessage<String, String> message = new KeyedMessage<String, String>(BUS.LIST_STREAMS_TOPIC, 			 								//topic 
																					STREAM_OPERATIONS.MANIPULATION.LIST,     							//key 
																					new Gson().toJson( new ListStreamsMessage(getSiddhiManager().getStreamDefinitions().size(), //value.count
																																System.currentTimeMillis(), 					//value.time
																																streams)));                                		//value.streams																				
			producer.send(message);
			
		}
		
		
		
		return null;
	}
	
	
	private ProducerConfig createProducerConfig() {
		Properties properties = new Properties();
		properties.put("serializer.class", "kafka.serializer.StringEncoder");
		properties.put("metadata.broker.list", kafkaCluster);
//		properties.put("request.required.acks", "1");
//		properties.put("compress", "true");
//		properties.put("compression.codec", "gzip");
//		properties.put("producer.type", "sync");
 
        return new ProducerConfig(properties);
    }

}
