package mil.nga.geopackage.user;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.db.CoreSQLUtils;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.sf.GeometryType;

/**
 * Reads the metadata from an existing user table
 * 
 * @param <TColumn>
 *            column type
 * @param <TTable>
 *            table type
 * 
 * @author osbornb
 */
public abstract class UserCoreTableReader<TColumn extends UserColumn, TTable extends UserTable<TColumn>, TRow extends UserCoreRow<TColumn, TTable>, TResult extends UserCoreResult<TColumn, TTable, TRow>> {

	/**
	 * Logger
	 */
	private static final Logger logger = Logger
			.getLogger(UserCoreTableReader.class.getName());

	/**
	 * Index column
	 */
	public static final String CID = "cid";

	/**
	 * Name column
	 */
	public static final String NAME = "name";

	/**
	 * Type column
	 */
	public static final String TYPE = "type";

	/**
	 * Not null column
	 */
	public static final String NOT_NULL = "notnull";

	/**
	 * Primary key column
	 */
	public static final String PK = "pk";

	/**
	 * Default value column
	 */
	public static final String DFLT_VALUE = "dflt_value";

	/**
	 * Table name
	 */
	private final String tableName;

	/**
	 * Constructor
	 * 
	 * @param tableName
	 *            table name
	 */
	protected UserCoreTableReader(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Create the table
	 * 
	 * @param tableName
	 *            table name
	 * @param columnList
	 *            column list
	 * @return table
	 */
	protected abstract TTable createTable(String tableName,
			List<TColumn> columnList);

	/**
	 * Create the column
	 * 
	 * @param result
	 *            result
	 * @param index
	 *            column index
	 * @param name
	 *            column name
	 * @param type
	 *            data type
	 * @param max
	 *            max value
	 * @param notNull
	 *            not null flag
	 * @param defaultValueIndex
	 *            default value index
	 * @param primaryKey
	 *            primary key flag
	 * @return column
	 */
	protected abstract TColumn createColumn(TResult result, int index,
			String name, String type, Long max, boolean notNull,
			int defaultValueIndex, boolean primaryKey);

	/**
	 * Read the table
	 * 
	 * @param db
	 *            user connection
	 * @return table
	 */
	public TTable readTable(
			UserCoreConnection<TColumn, TTable, TRow, TResult> db) {

		List<TColumn> columnList = new ArrayList<TColumn>();

		TResult result = db.rawQuery(
				"PRAGMA table_info(" + CoreSQLUtils.quoteWrap(tableName) + ")",
				null);
		try {
			while (result.moveToNext()) {
				int index = result.getInt(result.getColumnIndex(CID));
				String name = result.getString(result.getColumnIndex(NAME));
				String type = result.getString(result.getColumnIndex(TYPE));
				boolean notNull = result
						.getInt(result.getColumnIndex(NOT_NULL)) == 1;
				boolean primaryKey = result.getInt(result.getColumnIndex(PK)) == 1;

				// If the type has a max limit on it, pull it off
				Long max = null;
				if (type != null && type.endsWith(")")) {
					int maxStart = type.indexOf("(");
					if (maxStart > -1) {
						String maxString = type.substring(maxStart + 1,
								type.length() - 1);
						if (!maxString.isEmpty()) {
							try {
								max = Long.valueOf(maxString);
								type = type.substring(0, maxStart);
							} catch (NumberFormatException e) {
								logger.log(Level.WARNING,
										"Failed to parse type max from type: "
												+ type, e);
							}
						}
					}
				}

				// Get the geometry or data type and default value
				int defaultValueIndex = result.getColumnIndex(DFLT_VALUE);

				TColumn column = createColumn(result, index, name, type, max,
						notNull, defaultValueIndex, primaryKey);
				columnList.add(column);
			}
		} finally {
			result.close();
		}
		if (columnList.isEmpty()) {
			throw new GeoPackageException("Table does not exist: " + tableName);
		}

		return createTable(tableName, columnList);
	}

	/**
	 * Get the data type of the string type.
	 * 
	 * Geometries are converted to blobs
	 * 
	 * @param type
	 *            data type string
	 * @return data type
	 * @since 3.0.1
	 */
	public GeoPackageDataType getDataType(String type) {

		GeoPackageDataType dataType = null;

		try {
			dataType = GeoPackageDataType.fromName(type);
		} catch (IllegalArgumentException dataTypeException) {
			try {
				// Check if a geometry and convert to a blob
				GeometryType.fromName(type);
				dataType = GeoPackageDataType.BLOB;
			} catch (IllegalArgumentException geometryException) {
				throw new GeoPackageException("Unsupported column data type "
						+ type, dataTypeException);
			}
		}

		return dataType;
	}

}
