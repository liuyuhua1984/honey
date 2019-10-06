/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.ObjSQLIllegalSQLStringException;
import org.teasoft.bee.osql.ObjToSQLRich;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorFieldException;

/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjectToSQLRich extends ObjectToSQL implements ObjToSQLRich {

	private DbFeature dbFeature = BeeFactory.getHoneyFactory().getDbDialect();
	private static final String ASC = "asc";

	@Override
	public <T> String toSelectSQL(T entity, int size) {
//		String sql=dbFeature.toPageSql(toSelectSQL(entity), size);

		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
		sql = dbFeature.toPageSql(sql, size)+";";

		setPreparedValue(sql, wrap);
		Logger.logSQL("select SQL(entity,size): ", sql);
		return sql;
	}

	@Override
	public <T> String toSelectSQL(T entity, int from, int size) {

		// String sql=dbFeature.toPageSql(toSelectSQL(entity), from, size);
		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
		sql = dbFeature.toPageSql(sql, from, size)+";";

		setPreparedValue(sql, wrap);

		Logger.logSQL("select(entity,from,size) SQL:", sql);
		return sql;
	}
	
	@Override
	public <T> String toSelectSQL(T entity,String selectFields,int from,int size){
	
		SqlValueWrap wrap = toSelectSQL_0(entity,selectFields);
		String sql = wrap.getSql();
		sql = dbFeature.toPageSql(sql, from, size)+";";

		setPreparedValue(sql, wrap);

		Logger.logSQL("select(entity,selectFields,from,size) SQL:", sql);
		return sql;
	}
	
	

	@Override
	public <T> String toSelectSQL(T entity, String fieldList) throws ObjSQLException {

		String newSelectFields=checkSelectField(entity,fieldList);
		
//		String sql=_ObjectToSQLHelper._toSelectSQL(entity);
		String sql = _ObjectToSQLHelper._toSelectSQL(entity, newSelectFields);

//		sql=sql.replace("#fieldNames#", fieldList);
//		sql=sql.replace("#fieldNames#", newSelectFields);  //TODO 打印值会有问题

		Logger.logSQL("select SQL(selectFields) :", sql);

		return sql;
	}

	@Override
	public <T> String toSelectOrderBySQL(T entity, String orderFieldList) throws ObjSQLException {

		String orderFields[] = orderFieldList.split(",");
		int lenA = orderFields.length;

		String orderBy = "";
		for (int i = 0; i < lenA; i++) {
			orderBy += orderFields[i] + " " + ASC;
			if (i < lenA - 1) orderBy += ",";
		}
		
//		String sql=toSelectSQL(entity);
		SqlValueWrap wrap=toSelectSQL_0(entity);
		String sql=wrap.getSql();
//		sql=sql.replace(";", " "); //close on 2019-04-27
		sql+="order by "+orderBy+" ;";
		
		setPreparedValue(sql,wrap);
		
		return sql;
	}

	@Override
	public <T> String toSelectOrderBySQL(T entity, String orderFieldList, OrderType[] orderTypes) throws ObjSQLException {

		String orderFields[] = orderFieldList.split(",");
		int lenA = orderFields.length;

		if (lenA != orderTypes.length) throw new ObjSQLException("ObjSQLException :The lenth of orderField is not equal orderTypes'.");

		String orderBy = "";
		for (int i = 0; i < lenA; i++) {
			orderBy += orderFields[i] + " " + orderTypes[i].getName();
			if (i < lenA - 1) orderBy += ",";
		}

		//		String sql=toSelectSQL(entity);
		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
//		sql = sql.replace(";", " "); //close on 2019-04-27
		sql += "order by " + orderBy + " ;";

		setPreparedValue(sql, wrap);

		return sql;
	}

	@Override
	public <T> String toUpdateSQL(T entity, String updateFieldList) throws ObjSQLException {
		if (updateFieldList == null) return null;

		String sql = "";
		try {
			String updateFields[] = updateFieldList.split(",");

			if (updateFields.length == 0 || "".equals(updateFieldList.trim())) throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

			sql = _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, -1);
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;
	}

	@Override
	public <T> String toUpdateSQL(T entity, String updateFieldList, IncludeType includeType) throws ObjSQLException {
		if (updateFieldList == null) return null;

		String sql = "";
		try {
			String updateFields[] = updateFieldList.split(",");

			if (updateFields.length == 0 || "".equals(updateFieldList.trim())) throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

			sql = _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, includeType.getValue());
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;
	}

	@Override
	public <T> String toSelectFunSQL(T entity, FunctionType functionType,String fieldForFun) throws ObjSQLException {
		return _toSelectFunSQL(entity,functionType.getName(),fieldForFun);
	}

	//	 select max(bookPrice) from gen3 where =7 and name='newName' and =0.03 and aTest='test3-new' ;
	private <T> String _toSelectFunSQL(T entity, String funType,String fieldForFun) throws ObjSQLException {
		if (fieldForFun == null || funType == null) return null;
		boolean isContainField = false;
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		String sql = null;
		try {
			String tableName = ConverString.getTableName(entity);
			String selectAndFun;
			if ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun))
				//		    selectAndFun = " select " + funType + "(" + fieldForFun + ") from ";  //  count(*)
				selectAndFun = "select count(*) from ";
			else
				selectAndFun = "select " + funType + "(" + transformStr(fieldForFun) + ") from ";

			sqlBuffer.append(selectAndFun);
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			Field fields[] = entity.getClass().getDeclaredFields(); // 改为以最高权限访问？2012-07-15
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);

			if (fields[i].get(entity) == null|| "serialVersionUID".equals(fields[i].getName())) {// 要排除没有设值的情况
//				if (fields[i].getName().equals(fieldForFun)) {
				if ( (fields[i].getName().equals(fieldForFun))
			     || ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun)) ) {  //排除count(*)
					isContainField = true;
				}
				continue;
				} else {
					if (fields[i].getName().equals(fieldForFun)) {
						isContainField = true;
					}

					if (firstWhere) {
						sqlBuffer.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" and ");
					}
					sqlBuffer.append(transformStr(fields[i].getName()));

					sqlBuffer.append("=");
					sqlBuffer.append("?");

					valueBuffer.append(",");
					valueBuffer.append(fields[i].get(entity));

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					list.add(k++, preparedValue);
				}
			}

			sqlBuffer.append(" ;");
			sql = sqlBuffer.toString();

			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sql, list);
			HoneyContext.setSqlValue(sql, valueBuffer.toString());
			addInContextForCache(sql, valueBuffer.toString(), tableName);
			

			if (SqlStrFilter.checkFunSql(sql, funType)) {
				throw new ObjSQLIllegalSQLStringException("ObjSQLIllegalSQLStringException:sql statement with function is illegal. " + sql);
			}
			Logger.logSQL("select fun SQL :", sql);
			if (!isContainField) throw new ObjSQLException("ObjSQLException:Miss The Field! The entity(" + tableName + ") don't contain the field:" + fieldForFun);

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}

	@Override
	public <T> String toSelectSQL(T entity, IncludeType includeType) {
		return _ObjectToSQLHelper._toSelectSQL(entity, includeType.getValue());
	}

	@Override
	public <T> String toDeleteSQL(T entity, IncludeType includeType) {
		return _ObjectToSQLHelper._toDeleteSQL(entity, includeType.getValue());
	}

	@Override
	public <T> String toInsertSQL(T entity, IncludeType includeType) {
		String sql = null;
		try {
			sql = _ObjectToSQLHelper._toInsertSQL(entity, includeType.getValue());
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;

	}

	@Override
	public <T> String toUpdateSQL(T entity, IncludeType includeType) {
		String sql = "";
		try {
			sql = _ObjectToSQLHelper._toUpdateSQL(entity, "id", includeType.getValue());
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		} catch (ObjSQLException e) {
			throw e;
		}
		return sql;

	}

	@Override
	public <T> String[] toInsertSQL(T entity[]) {
		return toInsertSQL(entity, "");
	}

	private static String index1 = "[index";
	private static String index2 = "]";

	@Override
	public <T> String[] toInsertSQL(T entity[], String excludeFieldList) {
		String sql[] = null;
		try {
			int len = entity.length;
			sql = new String[len];
			String t_sql = "";
			SqlValueWrap wrap;

			wrap = _ObjectToSQLHelper._toInsertSQL0(entity[0], 2, excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
			t_sql = wrap.getSql();
			sql[0] = t_sql;
			t_sql = t_sql + "[index0]";
			setPreparedValue(t_sql, wrap);
			Logger.logSQL("insert[] SQL :", t_sql);

			for (int i = 1; i < len; i++) { // i=1
				wrap = _ObjectToSQLHelper._toInsertSQL_for_ValueList(entity[i], excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
				//				t_sql = wrap.getSql(); //  每个sql不一定一样,因为设值不一样,有些字段不用转换. 不采用;因为不利于批处理

				setPreparedValue_ForArray(sql[0] + index1 + i + index2, wrap);
				Logger.logSQL("insert[] SQL :", sql[0] + index1 + i + index2);
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}
	
	@Override
	public String toDeleteByIdSQL(Class c, Integer id) {
		if(id==null) return null;
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Integer");
	}
	
	@Override
	public String toDeleteByIdSQL(Class c, Long id) {
		if(id==null) return null;
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Long");
	}

	@Override
	public String toDeleteByIdSQL(Class c, String ids) {
		if(ids==null || "".equals(ids.trim())) return null;
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer,ids);
	}

	private  SqlValueWrap toDeleteByIdSQL0(Class c){
		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		
		String tableName = ConverString.getTableName(c);
		
		sqlBuffer.append("delete from ")
		.append(tableName)
		.append(" where ")
		;
		
		wrap.setValueBuffer(sqlBuffer); //sqlBuffer
		wrap.setTableNames(tableName);
		
		return wrap;
	}
	
	@Override
	public <T> String toSelectByIdSQL(T entity, Integer id) {
		
		SqlValueWrap sqlBuffer = toSelectByIdSQL0(entity);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Integer");
	}

	@Override
	public <T> String toSelectByIdSQL(T entity, Long id) {
		SqlValueWrap sqlBuffer = toSelectByIdSQL0(entity);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Long");
	}

	@Override
	public <T> String toSelectByIdSQL(T entity, String ids) {
		if(ids==null || "".equals(ids.trim())) return null;
		SqlValueWrap sqlBuffer=toSelectByIdSQL0(entity);
		return _toSelectAndDeleteByIdSQL(sqlBuffer,ids);
	}

	private <T> String _toSelectAndDeleteByIdSQL(SqlValueWrap wrap, Number id,String numType) {
		if(id==null) return null;
		
		StringBuffer sqlBuffer=wrap.getValueBuffer();  //sqlBuffer
		
//		StringBuffer sqlBuffer=toSelectByIdSQL0(entity);
		sqlBuffer.append("id=").append("?").append(";");

		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		preparedValue = new PreparedValue();
		preparedValue.setType(numType);
		preparedValue.setValue(id);
		list.add(preparedValue);
		
		HoneyContext.setPreparedValue(sqlBuffer.toString(), list);
		HoneyContext.setSqlValue(sqlBuffer.toString(), id+""); //用于log显示
		addInContextForCache(sqlBuffer.toString(), id+"", wrap.getTableNames());
		
		return sqlBuffer.toString();
	}
	
	private <T> String _toSelectAndDeleteByIdSQL(SqlValueWrap wrap, String ids) {
		
		StringBuffer sqlBuffer =wrap.getValueBuffer(); //sqlBuffer
		
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		
		String idArray[]=ids.split(",");
		String t_ids="id=?";
		
		preparedValue = new PreparedValue();
//		preparedValue.setType(numType);//id的类型Object
		preparedValue.setValue(idArray[0]);
		list.add(preparedValue);
		
		for (int i = 1; i < idArray.length; i++) { //i from 1
			preparedValue = new PreparedValue();
			t_ids+=" or id=?";
//			preparedValue.setType(numType);//id的类型Object
			preparedValue.setValue(idArray[i]);
			list.add(preparedValue);
		}
		
		sqlBuffer.append(t_ids).append(";");
		
		HoneyContext.setPreparedValue(sqlBuffer.toString(), list);
		HoneyContext.setSqlValue(sqlBuffer.toString(), ids); //用于log显示
		addInContextForCache(sqlBuffer.toString(), ids, wrap.getTableNames());
		
		return sqlBuffer.toString();
	}
	
	private  <T> SqlValueWrap toSelectByIdSQL0(T entity){
		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		
//		StringBuffer valueBuffer = new StringBuffer();
//		try {
			String tableName = ConverString.getTableName(entity);
			Field fields[] = entity.getClass().getDeclaredFields();

			String packageAndClassName = entity.getClass().getName();
			String fieldNames = HoneyContext.getBeanField(packageAndClassName);
			if (fieldNames == null) {
				fieldNames = HoneyUtil.getBeanField(fields);
				HoneyContext.addBeanField(packageAndClassName, fieldNames);
			}

			sqlBuffer.append("select " + fieldNames + " from ");
			sqlBuffer.append(tableName)
			.append(" where ");
			
			wrap.setValueBuffer(sqlBuffer); //sqlBuffer
			wrap.setTableNames(tableName);
			
		return wrap;
	}

	private <T> SqlValueWrap toSelectSQL_0(T entity) {
		return toSelectSQL_0(entity,null);
	}
	private <T> SqlValueWrap toSelectSQL_0(T entity,String selectField) {

		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		try {
			String tableName = ConverString.getTableName(entity);
			Field fields[] = entity.getClass().getDeclaredFields(); //返回所有字段,包括公有和私有    
			String fieldNames ="";
			if (selectField != null && !"".equals(selectField.trim())) {
				fieldNames = checkSelectField(entity, selectField);
			} else {
				String packageAndClassName = entity.getClass().getName();
				fieldNames = HoneyContext.getBeanField(packageAndClassName);
				if (fieldNames == null) {
					fieldNames = HoneyUtil.getBeanField(fields);
					HoneyContext.addBeanField(packageAndClassName, fieldNames);
				}
			}
			sqlBuffer.append("select " + fieldNames + " from ");
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);
				if (fields[i].get(entity) == null || "serialVersionUID".equals(fields[i].getName()))
					continue;
				else {

					if (firstWhere) {
						sqlBuffer.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" and ");
					}
					sqlBuffer.append(HoneyUtil.transformStr(fields[i].getName()));

					sqlBuffer.append("=");
					sqlBuffer.append("?");

					valueBuffer.append(",");
					valueBuffer.append(fields[i].get(entity));

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					list.add(k++, preparedValue);
				}
			}

//			sqlBuffer.append(";");   //close on 2019-04-27

			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);

			wrap.setTableNames(tableName);//2019-09-29
			wrap.setSql(sqlBuffer.toString());
			wrap.setList(list);
			wrap.setValueBuffer(valueBuffer);

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return wrap;
	}

	// 转成带下画线的
	private String transformStr(String str) {
		return HoneyUtil.transformStr(str);
	}

	private void setPreparedValue(String sql, SqlValueWrap wrap) {
		HoneyContext.setPreparedValue(sql, wrap.getList());
		HoneyContext.setSqlValue(sql, wrap.getValueBuffer().toString());
		addInContextForCache(sql, wrap.getValueBuffer().toString(), wrap.getTableNames());
	}
	
	private void setPreparedValue_ForArray(String sql, SqlValueWrap wrap) {
		HoneyContext.setPreparedValue(sql, wrap.getList());
		HoneyContext.setSqlValue(sql, wrap.getValueBuffer().toString());
//		addInContextForCache(sql, wrap.getValueBuffer().toString(), wrap.getTableNames());
	}
	
	private <T> String checkSelectField(T entity,String fieldList){
		Field fields[] = entity.getClass().getDeclaredFields();
		String packageAndClassName = entity.getClass().getName();
		String fieldNames = HoneyContext.getBeanField(packageAndClassName);
		if (fieldNames == null) {
			fieldNames = HoneyUtil.getBeanField(fields);//获取属性名对应的DB字段名
			HoneyContext.addBeanField(packageAndClassName, fieldNames);
		}

		String errorField = "";
		boolean isFirstError = true;
		String selectFields[] = fieldList.split(",");
		String newSelectFields = "";
		boolean isFisrt = true;

		for (String s : selectFields) {

			if (!fieldNames.contains(transformStr(s))) {
				if (isFirstError) {
					errorField += s;
					isFirstError = false;
				} else {
					errorField += "," + s;
				}
			}
			if (isFisrt) {
				newSelectFields += transformStr(s);
				isFisrt = false;
			} else {
				newSelectFields += ", " + transformStr(s);
			}

		}//end for

		if (!"".equals(errorField)) throw new BeeErrorFieldException("ErrorField: " + errorField);
		
		return newSelectFields;
	}
	
   private static void addInContextForCache(String sql,String sqlValue, String tableName){
	   _ObjectToSQLHelper.addInContextForCache(sql, sqlValue, tableName);
	}
}