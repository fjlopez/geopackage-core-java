package mil.nga.geopackage.extension.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.geopackage.GeoPackageCore;
import mil.nga.geopackage.attributes.AttributesColumn;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.extension.BaseExtension;
import mil.nga.geopackage.extension.ExtensionScopeType;
import mil.nga.geopackage.extension.Extensions;
import mil.nga.geopackage.extension.NGAExtensions;
import mil.nga.geopackage.property.GeoPackageProperties;
import mil.nga.geopackage.property.PropertyConstants;
import mil.nga.geopackage.user.UserCoreDao;
import mil.nga.geopackage.user.UserCoreResult;
import mil.nga.geopackage.user.UserCoreRow;
import mil.nga.geopackage.user.UserUniqueConstraint;

/**
 * GeoPackage properties core extension for defining GeoPackage specific
 * properties, attributes, and metadata
 * 
 * @param <TGeoPackage>
 *            GeoPackage type
 * @param <TRow>
 *            row type
 * @param <TResult>
 *            result type
 * @param <TDao>
 *            DAO type
 * 
 * @author osbornb
 * @since 3.0.2
 */
public abstract class PropertiesCoreExtension<TGeoPackage extends GeoPackageCore, TRow extends UserCoreRow<?, ?>, TResult extends UserCoreResult<?, ?, TRow>, TDao extends UserCoreDao<?, ?, TRow, TResult>>
		extends BaseExtension {

	/**
	 * Extension author
	 */
	public static final String EXTENSION_AUTHOR = "nga";

	/**
	 * Extension name without the author
	 */
	public static final String EXTENSION_NAME_NO_AUTHOR = "properties";

	/**
	 * Extension, with author and name
	 */
	public static final String EXTENSION_NAME = Extensions.buildExtensionName(
			EXTENSION_AUTHOR, EXTENSION_NAME_NO_AUTHOR);

	/**
	 * Extension definition URL
	 */
	public static final String EXTENSION_DEFINITION = GeoPackageProperties
			.getProperty(PropertyConstants.EXTENSIONS, EXTENSION_NAME_NO_AUTHOR);

	/**
	 * Table name
	 */
	public static final String TABLE_NAME = EXTENSION_NAME;

	/**
	 * Property column
	 */
	public static final String COLUMN_PROPERTY = "property";

	/**
	 * Value column
	 */
	public static final String COLUMN_VALUE = "value";

	/**
	 * Constructor
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * 
	 */
	protected PropertiesCoreExtension(TGeoPackage geoPackage) {
		super(geoPackage);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TGeoPackage getGeoPackage() {
		return (TGeoPackage) geoPackage;
	}

	/**
	 * Get or create the extension
	 * 
	 * @return extension
	 */
	public Extensions getOrCreate() {

		// Create the attributes table
		if (!geoPackage.isTable(TABLE_NAME)) {

			AttributesColumn propertyColumn = AttributesColumn.createColumn(1,
					COLUMN_PROPERTY, GeoPackageDataType.TEXT, true, null);
			AttributesColumn valueColumn = AttributesColumn.createColumn(2,
					COLUMN_VALUE, GeoPackageDataType.TEXT, false, null);

			List<AttributesColumn> additionalColumns = new ArrayList<>();
			additionalColumns.add(propertyColumn);
			additionalColumns.add(valueColumn);

			List<UserUniqueConstraint<AttributesColumn>> uniqueConstraints = new ArrayList<>();
			uniqueConstraints.add(new UserUniqueConstraint<AttributesColumn>(
					propertyColumn, valueColumn));

			geoPackage.createAttributesTableWithId(TABLE_NAME,
					additionalColumns, uniqueConstraints);
		}

		Extensions extension = getOrCreate(EXTENSION_NAME, TABLE_NAME, null,
				EXTENSION_DEFINITION, ExtensionScopeType.READ_WRITE);

		return extension;
	}

	/**
	 * Determine if the GeoPackage has the extension
	 * 
	 * @return true if has extension
	 */
	public boolean has() {
		return has(EXTENSION_NAME, TABLE_NAME, null)
				&& geoPackage.isTable(TABLE_NAME);
	}

	/**
	 * Get the table Data Access Object
	 * 
	 * @return Data Access Object
	 */
	protected abstract TDao getDao();

	/**
	 * Get a new row
	 * 
	 * @return new row
	 */
	protected abstract TRow newRow();

	/**
	 * Get the number of properties
	 * 
	 * @return property count
	 */
	public int numProperties() {
		return getProperties().size();
	}

	/**
	 * Get the properties
	 * 
	 * @return list of properties
	 */
	public List<String> getProperties() {
		List<String> properties = null;
		if (has()) {
			properties = getDao().querySingleColumnStringResults(
					"SELECT DISTINCT " + COLUMN_PROPERTY + " FROM "
							+ TABLE_NAME, null);
		} else {
			properties = new ArrayList<>();
		}
		return properties;
	}

	/**
	 * Check if the property exists, same call as {@link #hasValues(String)}
	 * 
	 * @param property
	 *            property name
	 * @return true if has property
	 */
	public boolean hasProperty(String property) {
		return hasValues(property);
	}

	/**
	 * Get the number of total values combined for all properties
	 * 
	 * @return number of total property values
	 */
	public int numValues() {
		int count = 0;
		if (has()) {
			count = getDao().count();
		}
		return count;
	}

	/**
	 * Get the number of values for the property
	 * 
	 * @param property
	 *            property name
	 * @return number of values
	 */
	public int numValues(String property) {
		int count = 0;
		if (has()) {
			TResult result = queryForValues(property);
			try {
				count = result.getCount();
			} finally {
				result.close();
			}
		}
		return count;
	}

	/**
	 * Check if the property has a single value
	 * 
	 * @param property
	 *            property name
	 * @return true if has a single value
	 */
	public boolean hasSingleValue(String property) {
		return numValues(property) == 1;
	}

	/**
	 * Check if the property has any values
	 * 
	 * @param property
	 *            property name
	 * @return true if has any values
	 */
	public boolean hasValues(String property) {
		return numValues(property) > 0;
	}

	/**
	 * Get the first value for the property
	 * 
	 * @param property
	 *            property name
	 * @return value or null
	 */
	public String getValue(String property) {
		String value = null;
		List<String> values = getValues(property);
		if (!values.isEmpty()) {
			value = values.get(0);
		}
		return value;
	}

	/**
	 * Get the values for the property
	 * 
	 * @param property
	 *            property name
	 * @return list of values
	 */
	public List<String> getValues(String property) {
		return getValues(queryForValues(property));
	}

	/**
	 * Check if the property has the value
	 * 
	 * @param property
	 *            property name
	 * @param value
	 *            property value
	 * @return true if property has the value
	 */
	public boolean hasValue(String property, String value) {
		boolean hasValue = false;
		if (has()) {
			Map<String, Object> fieldValues = buildFieldValues(property, value);
			TResult result = getDao().queryForFieldValues(fieldValues);
			try {
				hasValue = result.getCount() > 0;
			} finally {
				result.close();
			}
		}
		return hasValue;
	}

	/**
	 * Add a property value, creating the extension if needed
	 * 
	 * @param property
	 *            property name
	 * @param value
	 *            value
	 * @return true if added, false if already existed
	 */
	public boolean addValue(String property, String value) {
		if (!has()) {
			getOrCreate();
		}
		boolean added = false;
		if (!hasValue(property, value)) {
			TRow row = newRow();
			row.setValue(COLUMN_PROPERTY, property);
			row.setValue(COLUMN_VALUE, value);
			getDao().insert(row);
			added = true;
		}
		return added;
	}

	/**
	 * Delete the property and all the property values
	 * 
	 * @param property
	 *            property name
	 * @return deleted values count
	 */
	public int deleteProperty(String property) {
		int count = 0;
		if (has()) {
			TDao dao = getDao();
			String where = dao.buildWhere(COLUMN_PROPERTY, property);
			String[] whereArgs = dao.buildWhereArgs(property);
			count = dao.delete(where, whereArgs);
		}
		return count;
	}

	/**
	 * Delete the property value
	 * 
	 * @param property
	 *            property name
	 * @param value
	 *            property value
	 * @return deleted values count
	 */
	public int deleteValue(String property, String value) {
		int count = 0;
		if (has()) {
			Map<String, Object> fieldValues = buildFieldValues(property, value);
			count = getDao().delete(fieldValues);
		}
		return count;
	}

	/**
	 * Delete all properties and values
	 * 
	 * @return deleted values count
	 */
	public int deleteAll() {
		int count = 0;
		if (has()) {
			count = getDao().deleteAll();
		}
		return count;
	}

	/**
	 * Remove the extension
	 */
	public void removeExtension() {
		NGAExtensions.deletePropertiesExtension(geoPackage);
	}

	/**
	 * Build field values from the property and value
	 * 
	 * @param property
	 *            property name
	 * @param value
	 *            property value
	 * @return field values mapping
	 */
	private Map<String, Object> buildFieldValues(String property, String value) {
		Map<String, Object> fieldValues = new HashMap<String, Object>();
		fieldValues.put(COLUMN_PROPERTY, property);
		fieldValues.put(COLUMN_VALUE, value);
		return fieldValues;
	}

	/**
	 * Query for the property values
	 * 
	 * @param property
	 *            property name
	 * @return result
	 */
	private TResult queryForValues(String property) {
		TResult result = null;
		if (has()) {
			result = getDao().queryForEq(COLUMN_PROPERTY, property);
		}
		return result;
	}

	/**
	 * Get the values from the results and close the results
	 * 
	 * @param results
	 *            results
	 * @return list of values
	 */
	private List<String> getValues(UserCoreResult<?, ?, ?> results) {

		List<String> values = null;
		if (results != null) {
			try {
				if (results.getCount() > 0) {
					int columnIndex = results.getColumnIndex(COLUMN_VALUE);
					values = getColumnResults(columnIndex, results);
				} else {
					values = new ArrayList<>();
				}
			} finally {
				results.close();
			}
		} else {
			values = new ArrayList<>();
		}

		return values;
	}

	/**
	 * Get the results of a column at the index and close the results
	 * 
	 * @param columnIndex
	 *            column index
	 * @param results
	 *            results
	 * @return list of column index values
	 */
	private List<String> getColumnResults(int columnIndex,
			UserCoreResult<?, ?, ?> results) {

		List<String> values = new ArrayList<>();
		while (results.moveToNext()) {
			values.add(results.getString(columnIndex));
		}

		return values;
	}

}
