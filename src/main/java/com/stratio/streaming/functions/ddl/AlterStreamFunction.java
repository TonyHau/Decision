package com.stratio.streaming.functions.ddl;

import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.api.exception.AttributeAlreadyExistException;
import org.wso2.siddhi.query.compiler.exception.SiddhiPraserException;

import com.stratio.streaming.commons.constants.REPLY_CODES;
import com.stratio.streaming.commons.messages.ColumnNameTypeValue;
import com.stratio.streaming.commons.messages.StratioStreamingMessage;
import com.stratio.streaming.functions.StratioStreamingBaseFunction;
import com.stratio.streaming.utils.SiddhiUtils;

public class AlterStreamFunction extends StratioStreamingBaseFunction {
	
	private static Logger logger = LoggerFactory.getLogger(AlterStreamFunction.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7911766880059394316L;

	public AlterStreamFunction(SiddhiManager siddhiManager, String zookeeperCluster, String kafkaCluster) {
		super(siddhiManager, zookeeperCluster, kafkaCluster);
	}
	

	@Override
	public Void call(JavaRDD<StratioStreamingMessage> rdd) throws Exception {
		
		List<StratioStreamingMessage> requests = rdd.collect();
		
		for (StratioStreamingMessage request : requests) {
//			Siddhi doesn't throw an exception if stream does not exist and you try to remove it
//			so there is no need to check if stream exists before dropping it.
			if (getSiddhiManager().getStreamDefinition(request.getStreamName()) != null) {
				
				try {
					
//					add colums to the stream in siddhi
					int addedColumns = enlargeStream(request);
															
//					ack OK back to the bus
					ackStreamingOperation(request, REPLY_CODES.OK);
					
				} catch (SiddhiPraserException se) {
					ackStreamingOperation(request, REPLY_CODES.KO_PARSER_ERROR);
				} catch (AttributeAlreadyExistException aoee) {
					ackStreamingOperation(request, REPLY_CODES.KO_COLUMN_ALREADY_EXISTS);
				} catch (Exception e) {
					ackStreamingOperation(request, REPLY_CODES.KO_GENERAL_ERROR);
				}
			}
			else {
				ackStreamingOperation(request, REPLY_CODES.KO_STREAM_DOES_NOT_EXIST);
			}
			
		}
		
		return null;
	}
	
	
	private int enlargeStream(StratioStreamingMessage request) {
		
		int addedColumns = 0;
		StreamDefinition streamMetaData = getSiddhiManager().getStreamDefinition(request.getStreamName());
		
		for (ColumnNameTypeValue columnNameTypeValue: request.getColumns()) {
			
//			Siddhi will throw an exception if you try to add a column that already exists, 
//			so we first try to find it in the stream
			if (!columnAlreadyExistsInStream(columnNameTypeValue.getColumn(), streamMetaData)) {
				
				addedColumns++;
				streamMetaData.attribute(columnNameTypeValue.getColumn(), SiddhiUtils.decodeSiddhiType(columnNameTypeValue.getType()));
				
			}
			else {
				throw new AttributeAlreadyExistException(columnNameTypeValue.getColumn());
			}
		}
		
		return addedColumns;

	}
	
	private boolean columnAlreadyExistsInStream(String columnName, StreamDefinition streamMetaData) {
		
		for(Attribute column:  streamMetaData.getAttributeList()) {
			if (column.getName().equalsIgnoreCase(columnName)) {
				return true;
			}
		}
		
		return false;
	}
	

	
	
}
