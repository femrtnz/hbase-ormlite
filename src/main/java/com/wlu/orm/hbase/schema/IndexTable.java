package com.wlu.orm.hbase.schema;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.client.Put;

import com.wlu.orm.hbase.schema.value.SerializedValue;
import com.wlu.orm.hbase.schema.value.NullValue;
import com.wlu.orm.hbase.schema.value.StringValue;
import com.wlu.orm.hbase.schema.value.Value;
import com.wlu.orm.hbase.schema.value.ValueFactory;

/**
 * @author Felipe Amaral - famaral@ciandt.com
 *
 * <br/>created at 08/26/2016
 *
 */
public class IndexTable {

	private static Log LOG = LogFactory.getLog(IndexTable.class);

	private List<Put> putList = new ArrayList<Put>();

	private String tableName;
	
	protected static final String INDEX_TABLE_SUFFIX = "Index";

	protected static final String INDEX_ROW_KEY = "key";
	
	public static final byte[] INDEX_TABLE_FAMILY_QUALIFIER = IndexTable.INDEX_ROW_KEY.getBytes();
    
	public static final String KEY_SPLIT = ":";
	
	public IndexTable(String tablename) {
		tableName = tablename + IndexTable.INDEX_TABLE_SUFFIX;
	}

	public void add(Field field, Value rowkey, FamilytoQualifersAndValues familytoQualifersAndValues) {
		LOG.info("Adding new Field on Index Table. Field name:" + field.getName());
		byte[] value = rowkey.toBytes();
		Map<byte[], Value> qualifierValue = familytoQualifersAndValues.getQualiferValue();
		for (byte[] qualifier : qualifierValue.keySet()) {
			Value v = qualifierValue.get(qualifier);
			//if the field used as 2 table index is null, escape it
			if(v instanceof NullValue)
				continue;
			else if(v instanceof SerializedValue){
				Object obj = ((SerializedValue)v).getObjectValue();
				if(obj instanceof ArrayList<?>){
					List<?> list = (ArrayList)obj;
					for (Object object : list) {
						if(object != null && !"null".equals(object.toString())){
							byte[] indexRowKey = generateIndexRowKey(field, new StringValue(object.toString()), rowkey); 
							addPut(new Put(indexRowKey).addColumn(INDEX_TABLE_FAMILY_QUALIFIER, INDEX_TABLE_FAMILY_QUALIFIER, value));
						}
					}
				} else {
					String jsonNodeValue = ((SerializedValue)v).getSerializedValue();
					if(!StringUtils.isBlank(jsonNodeValue) && !jsonNodeValue.equals("null")){
						byte[] indexRowKey = generateIndexRowKey(field, new StringValue(jsonNodeValue), rowkey); 
						addPut(new Put(indexRowKey).addColumn(INDEX_TABLE_FAMILY_QUALIFIER, INDEX_TABLE_FAMILY_QUALIFIER, value));
					}
				}
				
//				((JsonNodeValue)v).getJsonNodeValue().forEach(a -> {
//					String text = a.asText();
//					if(!StringUtils.isBlank(text) && !text.equals("null")){
//						byte[] indexRowKey = generateIndexRowKey(field, new StringValue(text), rowkey); 
//						addPut(new Put(indexRowKey).addColumn(INDEX_TABLE_FAMILY_QUALIFIER, INDEX_TABLE_FAMILY_QUALIFIER, value));
//					}
//				});
			} else {
				byte[] indexRowKey = generateIndexRowKey(field, v, rowkey); 
				addPut(new Put(indexRowKey).addColumn(INDEX_TABLE_FAMILY_QUALIFIER, INDEX_TABLE_FAMILY_QUALIFIER, value));
			}
		}
	}

	public byte[] generateIndexRowKey(Field field, Value value, Value rowkey) {
		Object obj = ValueFactory.getValue(value);
		return (field.getName() + KEY_SPLIT + (obj.toString()) + KEY_SPLIT + ((StringValue)rowkey).getStringValue()).getBytes();
	}

	public void addPut(Put put){
		putList.add(put);
	}

	public List<Put> getPut() {
		return putList;
	}

	public String getTableName() {
		return tableName;
	}
}
