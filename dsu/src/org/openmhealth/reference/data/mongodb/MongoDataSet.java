/*******************************************************************************
 * Copyright 2013 Open mHealth
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.openmhealth.reference.data.mongodb;

import java.util.List;

import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.openmhealth.reference.data.DataSet;
import org.openmhealth.reference.data.MultiValueResult;
import org.openmhealth.reference.data.mongodb.domain.MongoData;
import org.openmhealth.reference.data.mongodb.domain.MongoSchema;
import org.openmhealth.reference.domain.ColumnList;
import org.openmhealth.reference.domain.Data;
import org.openmhealth.reference.domain.MetaData;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

/**
 * The data associated with their schema as defined by the Open mHealth.
 *
 * @author John Jenkins
 */
public class MongoDataSet extends DataSet {
	/**
	 * Default constructor.
	 */
	protected MongoDataSet() {
		// Do nothing.
	}

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.DataSet#getData(java.lang.String, java.lang.String, long, org.openmhealth.reference.domain.ColumnList, java.lang.Long, java.lang.Long)
	 */
	@Override
	public MultiValueResult<? extends Data> getData(
		final String owner,
		final String schemaId,
		final long version,
		final ColumnList columnList,
		final Long numToSkip,
		final Long numToReturn) {
		
		// Get the connection to the database.
		DB db = MongoDao.getInstance().getDb();
		
		// Get the connection to the data with the Jackson wrapper.
		JacksonDBCollection<MongoData, Object> collection =
			JacksonDBCollection
				.wrap(db.getCollection(DB_NAME), MongoData.class);
		
		// Build the query.
		QueryBuilder queryBuilder = QueryBuilder.start();
		
		// Only select data for a single user.
		queryBuilder.and(MongoData.JSON_KEY_OWNER).is(owner);
		
		// Only select data for a given schema.
		queryBuilder.and(MongoSchema.JSON_KEY_ID).is(schemaId);
		
		// Only select data for a given version of the the given schema.
		queryBuilder.and(MongoSchema.JSON_KEY_VERSION).is(version);
		
		// Create the projection.
		DBObject projection = new BasicDBObject();
		// Add the owner field.
		projection.put(MongoData.JSON_KEY_OWNER, 1);
		// Add the schema ID field.
		projection.put(MongoSchema.JSON_KEY_ID, 1);
		// Add the schema version.
		projection.put(MongoSchema.JSON_KEY_VERSION, 1);
		// Add the meta-data field.
		projection.put(MongoData.JSON_KEY_METADATA, 1);
		// Add all of the data or add only the specified columns if given.
		if(columnList.size() == 0) {
			projection.put(MongoData.JSON_KEY_DATA, 1);
		}
		else {
			if((columnList != null) && (columnList.size() > 0)) {
				for(String column : columnList.toList()) {
					projection
						.put(
							MongoData.JSON_KEY_DATA +
								ColumnList.COLUMN_SEPARATOR +
								column,
							1);
				}
			}
		}
		
		// Build the query.
		DBCursor<MongoData> result =
			collection.find(queryBuilder.get(), projection);
		
		// Build the sort field by sorting in reverse chronological order.
		DBObject sort = new BasicDBObject();
		sort.put(
			MongoData.JSON_KEY_METADATA + 
				ColumnList.COLUMN_SEPARATOR + 
				MetaData.JSON_KEY_TIMESTAMP,
			-1);
		result.sort(sort);
		
		return 
			new MongoMultiValueResult<MongoData>(
				result
					.skip((new Long(numToSkip)).intValue())
					.limit((new Long(numToReturn)).intValue()));
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.DataSet#setData(java.util.List)
	 */
	@Override
	public void setData(final List<Data> data) {
		// Get the connection to the database.
		DB db = MongoDao.getInstance().getDb();
		
		// Get the connection to the data with the Jackson wrapper.
		JacksonDBCollection<Data, Object> collection =
			JacksonDBCollection
				.wrap(db.getCollection(DB_NAME), Data.class);
		
		// Insert the data.
		collection.insert(data);
	}
}
