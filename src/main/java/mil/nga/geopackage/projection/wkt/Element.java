/* Copyright 2013 The jeo project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package mil.nga.geopackage.projection.wkt;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.LogRecord;

/**
 * An element in a <cite>Well Know Text</cite> (WKT). A {@code Element} is made
 * of {@link String}, {@link Number} and other {@link Element}. For example:
 *
 * <blockquote>
 * 
 * <pre>
 * PRIMEM["Greenwich", 0.0, AUTHORITY["some authority", "Greenwich"]]
 * </pre>
 * 
 * </blockquote>
 *
 * Each {@code Element} object can contains an arbitrary amount of other
 * elements. Elements can be pull in a <cite>first in, first out</cite> order.
 *
 * @since 2.0
 *
 *
 * @source $URL$
 * @version $Id$
 * @author Remi Eve
 * @author Martin Desruisseaux (IRD)
 */
public final class Element {

	/**
	 * The locale for querying localizable information.
	 */
	public static final Locale locale = Locale.US;

	/**
	 * The character used for closing brace. Usually {@code ']'}, but
	 * {@code ')'} is legal as well.
	 */
	private static final char close = ']';

	/**
	 * The character used for quote. Usually {@code '"'}.
	 */
	private static final char quote = '"';

	/**
	 * The character used as a separator. Usually {@code ','}, but would need to
	 * be changed if a non-English locale is used for formatting numbers.
	 */
	private static final char separator = ',';

	/**
	 * List of characters acceptable as opening bracket. The closing bracket
	 * must be the character in the {@code closingBrackets} array at the same
	 * index than the opening bracket.
	 */
	private static final char[] openingBrackets = { '[', '(' };

	/**
	 * List of characters acceptable as closing bracket.
	 */
	private static final char[] closingBrackets = { ']', ')' };

	private final String text;

	/**
	 * The position where this element starts in the string to be parsed.
	 */
	private final int offset;

	/**
	 * Keyword of this entity. For example: "PRIMEM".
	 */
	public final String keyword;

	/**
	 * An ordered list of {@link String}s, {@link Number}s and other
	 * {@link Element}s. May be {@code null} if the keyword was not followed by
	 * a pair of brackets (e.g. "NORTH").
	 */
	private final List<Object> list;

	public Element(final String text) throws ParseException {
		this(text, new ParsePosition(0));
	}

	/**
	 * Constructs a new {@code Element}.
	 *
	 * @param text
	 *            The text to parse.
	 * @param position
	 *            In input, the position where to start parsing from. In output,
	 *            the first character after the separator.
	 */
	public Element(final String text, final ParsePosition position)
			throws ParseException {
		this.text = text;

		/*
		 * Find the first keyword in the specified string. If a keyword is
		 * found, then the position is set to the index of the first character
		 * after the keyword.
		 */
		int lower = position.getIndex();
		final int length = text.length();
		while (lower < length && Character.isWhitespace(text.charAt(lower))) {
			lower++;
		}
		offset = lower;
		int upper = lower;
		while (upper < length
				&& Character.isUnicodeIdentifierPart(text.charAt(upper))) {
			upper++;
		}
		if (upper <= lower) {
			position.setErrorIndex(lower);
			throw unparsableString(text, position);
		}
		keyword = text.substring(lower, upper).toUpperCase(locale);
		position.setIndex(upper);
		/*
		 * Parse the opening bracket. According CTS's specification, two
		 * characters are acceptable: '[' and '('. At the end of this method, we
		 * will require the matching closing bracket. For example if the opening
		 * bracket was '[', then we will require that the closing bracket is ']'
		 * and not ')'.
		 */
		int bracketIndex = -1;
		do {
			if (++bracketIndex >= openingBrackets.length) {
				list = null;
				return;
			}
		} while (!parseOptionalSeparator(text, position,
				openingBrackets[bracketIndex]));
		list = new LinkedList<Object>();

		NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
		numberFormat.setGroupingUsed(false);
		numberFormat.setMinimumFractionDigits(1);
		numberFormat.setMaximumFractionDigits(20);

		/*
		 * Parse all elements inside the bracket. Elements are parsed
		 * sequentially and their type are selected according their first
		 * character:
		 * 
		 * - If the first character is a quote, then the element is parsed as a
		 * String. - Otherwise, if the first character is a unicode identifier
		 * start, then the element is parsed as a chidren Element. - Otherwise,
		 * the element is parsed as a number.
		 */
		do {
			if (position.getIndex() >= length) {
				throw missingCharacter(close, length);
			}
			//
			// Try to parse the next element as a quoted string. We will take
			// it as a string if the first non-blank character is a quote.
			//
			if (parseOptionalSeparator(text, position, quote)) {
				lower = position.getIndex();
				upper = text.indexOf(quote, lower);
				if (upper < lower) {
					position.setErrorIndex(++lower);
					throw missingCharacter(quote, lower);
				}
				list.add(text.substring(lower, upper).trim());
				position.setIndex(upper + 1);
				continue;
			}
			//
			// Try to parse the next element as a number. We will take it as a
			// number if
			// the first non-blank character is not the begining of an unicode
			// identifier.
			//
			lower = position.getIndex();
			if (!Character.isUnicodeIdentifierStart(text.charAt(lower))) {
				final Number number = parseNumber(text, position, numberFormat);
				if (number == null) {
					// Do not update the error index; it is already updated by
					// NumberFormat.
					throw unparsableString(text, position);
				}
				list.add(number);
				continue;
			}
			// Otherwise, add the element as a child element.
			list.add(new Element(text, position));
		} while (parseOptionalSeparator(text, position, separator));
		parseSeparator(text, position, closingBrackets[bracketIndex]);
	}

	public String getText() {
		return text;
	}

	private final Number parseNumber(String text, final ParsePosition position,
			NumberFormat numberFormat) {
		final int base = position.getIndex();
		Number number = numberFormat.parse(text, position);
		if (number != null) {
			int i = position.getIndex();
			if (i < text.length() && text.charAt(i) == 'e') {
				final StringBuilder buffer = new StringBuilder(text);
				buffer.setCharAt(i, 'E');
				text = buffer.toString();
				position.setIndex(base);
				number = numberFormat.parse(text, position);
			}
		}
		return number;
	}

	/**
	 * Returns {@code true} if the next non-whitespace character is the
	 * specified separator. Search is performed in string {@code text} from
	 * position {@code position}. If the separator is found, then the position
	 * is set to the first character after the separator. Otherwise, the
	 * position is set on the first non-blank character.
	 *
	 * @param text
	 *            The text to parse.
	 * @param position
	 *            In input, the position where to start parsing from. In output,
	 *            the first character after the separator.
	 * @param separator
	 *            The character to search.
	 * @return {@code true} if the next non-whitespace character is the
	 *         separator, or {@code false} otherwise.
	 */
	private static boolean parseOptionalSeparator(final String text,
			final ParsePosition position, final char separator) {
		final int length = text.length();
		int index = position.getIndex();
		while (index < length) {
			final char c = text.charAt(index);
			if (Character.isWhitespace(c)) {
				index++;
				continue;
			}
			if (c == separator) {
				position.setIndex(++index);
				return true;
			}
			break;
		}
		position.setIndex(index); // MANDATORY for correct working of the
									// constructor.
		return false;
	}

	/**
	 * Moves to the next non-whitespace character and checks if this character
	 * is the specified separator. If the separator is found, it is skipped.
	 * Otherwise, this method thrown a {@link ParseException}.
	 *
	 * @param text
	 *            The text to parse.
	 * @param position
	 *            In input, the position where to start parsing from. In output,
	 *            the first character after the separator.
	 * @param separator
	 *            The character to search.
	 * @throws ParseException
	 *             if the separator was not found.
	 */
	private void parseSeparator(final String text,
			final ParsePosition position, final char separator)
			throws ParseException {
		if (!parseOptionalSeparator(text, position, separator)) {
			position.setErrorIndex(position.getIndex());
			throw unparsableString(text, position);
		}
	}

	/**
	 * Returns a {@link ParseException} with the specified cause. A localized
	 * string <code>"Error in <{@link #keyword}>"</code> will be prepend to the
	 * message. The error index will be the starting index of this
	 * {@code Element}.
	 *
	 * @param cause
	 *            The cause of the failure, or {@code null} if none.
	 * @param message
	 *            The message explaining the cause of the failure, or
	 *            {@code null} for reusing the same message than {@code cause}.
	 * @return The exception to be thrown.
	 */
	public ParseException parseFailed(final Exception cause, String message) {
		if (message == null) {
			message = cause.getLocalizedMessage();
		}
		ParseException exception = new ParseException(complete(message), offset);
		exception.initCause(cause);
		return exception;
	}

	/**
	 * Returns a {@link ParseException} with a "Unparsable string" message. The
	 * error message is built from the specified string starting at the
	 * specified position. Properties {@link ParsePosition#getIndex} and
	 * {@link ParsePosition#getErrorIndex} must be accurate before this method
	 * is invoked.
	 *
	 * @param text
	 *            The unparsable string.
	 * @param position
	 *            The position in the string.
	 * @return An exception with a formatted error message.
	 */
	private ParseException unparsableString(final String text,
			final ParsePosition position) {
		final int errorIndex = position.getErrorIndex();
		String message = unparsableStringMsg(text, position.getIndex(),
				errorIndex);

		message = complete(message);
		return new ParseException(message, errorIndex);
	}

	/**
	 * Implementation of {@code formatUnparsable} methods. Returns a
	 * {@link LogRecord} if {@code level} is non-null, or a {@link String}
	 * otherwise.
	 */
	private String unparsableStringMsg(String text, final int index,
			int errorIndex) {

		final int length = text.length();
		if (errorIndex < index) {
			errorIndex = index;
		}
		if (errorIndex == length) {
			return "Unexpected end of string";
		}

		int upper = errorIndex;
		if (upper < length) {
			final int type = Character.getType(text.charAt(upper));
			while (++upper < length) {
				if (Character.getType(text.charAt(upper)) != type) {
					break;
				}
			}
		}
		final String error = text.substring(errorIndex, upper);
		text = text.substring(index);

		return String.format(Locale.ROOT,
				"Can't parse \"%s\" because \"%s\" is unrecognized.", text,
				error);
	}

	/**
	 * Returns an exception saying that a character is missing.
	 *
	 * @param c
	 *            The missing character.
	 * @param position
	 *            The error position.
	 */
	private ParseException missingCharacter(final char c, final int position) {
		return new ParseException(complete(String.format(Locale.ROOT,
				"Character '%s' was expected", c)), position);
	}

	/**
	 * Returns an exception saying that a parameter is missing.
	 *
	 * @param key
	 *            The name of the missing parameter.
	 */
	private ParseException missingParameter(final String key) {
		int error = offset;
		if (keyword != null) {
			error += keyword.length();
		}
		return new ParseException(complete(String.format(Locale.ROOT,
				"Parameter \"%s\" is missing.", key)), error);
	}

	/**
	 * Append a prefix "Error in <keyword>: " before the error message.
	 *
	 * @param message
	 *            The message to complete.
	 * @return The completed message.
	 */
	private String complete(String message) {
		return String.format(Locale.ROOT, "%s, WKT: %s", message, text);
	}

	/**
	 * Returns {@code true} if this element is the root element. For example in
	 * a WKT like {@code "GEOGCS["name", DATUM["name, ...]]"}, this is true for
	 * {@code "GEOGCS"} and false for all other elements inside, like
	 * {@code "DATUM"}.
	 *
	 * @return {@code true} if this element is the root element.
	 *
	 * @since 2.3
	 */
	public boolean isRoot() {
		return this.offset == 0;
	}

	/**
	 * Removes the next {@link Number} from the list and returns it.
	 *
	 * @param key
	 *            The parameter name. Used for formatting an error message if no
	 *            number are found.
	 * @return The next {@link Number} on the list as a {@code double}.
	 * @throws ParseException
	 *             if no more number is available.
	 */
	public double pullDouble(final String key) throws ParseException {
		final Iterator<Object> iterator = list.iterator();
		while (iterator.hasNext()) {
			final Object object = iterator.next();
			if (object instanceof Number) {
				iterator.remove();
				return ((Number) object).doubleValue();
			}
		}
		throw missingParameter(key);
	}

	/**
	 * Removes the next {@link Number} from the list and returns it as an
	 * integer.
	 *
	 * @param key
	 *            The parameter name. Used for formatting an error message if no
	 *            number are found.
	 * @return The next {@link Number} on the list as an {@code int}.
	 * @throws ParseException
	 *             if no more number is available, or the number is not an
	 *             integer.
	 */
	public int pullInteger(final String key) throws ParseException {
		final Iterator<Object> iterator = list.iterator();
		while (iterator.hasNext()) {
			final Object object = iterator.next();
			if (object instanceof Number) {
				iterator.remove();
				final Number number = (Number) object;
				if (number instanceof Float || number instanceof Double) {
					throw new ParseException(complete(String.format(
							Locale.ROOT, "Illegal argument: \"%s=%f\"", key,
							number)), offset);
				}
				return number.intValue();
			}
		}
		throw missingParameter(key);
	}

	/**
	 * Removes the next {@link String} from the list and returns it.
	 *
	 * @param key
	 *            The parameter name. Used for formatting an error message if no
	 *            number are found.
	 * @return The next {@link String} on the list.
	 * @throws ParseException
	 *             if no more string is available.
	 */
	public String pullString(final String key) throws ParseException {
		String optionalString = pullOptionalString(key);
		if (optionalString != null) {
			return optionalString;
		}
		throw missingParameter(key);
	}

	/**
	 * Removes the next {@link String} from the list and returns it.
	 * 
	 * @param key
	 *            The parameter name. Used for formatting an error message if no
	 *            number are found.
	 * @return The next {@link String} on the list or {@code null} if no more
	 *         element is available.
	 */
	public String pullOptionalString(final String key) {
		final Iterator<Object> iterator = list.iterator();
		while (iterator.hasNext()) {
			final Object object = iterator.next();
			if (object instanceof String) {
				iterator.remove();
				return (String) object;
			}
		}
		return null;
	}

	/**
	 * Removes the next {@link Element} from the list and returns it.
	 *
	 * @param key
	 *            The element name (e.g. <code>"PRIMEM"</code>).
	 * @return The next {@link Element} on the list.
	 * @throws ParseException
	 *             if no more element is available.
	 */
	public Element pullElement(final String key) throws ParseException {
		final Element element = pullOptionalElement(key);
		if (element != null) {
			return element;
		}
		throw missingParameter(key);
	}

	/**
	 * Removes the next {@link Element} from the list and returns it.
	 *
	 * @param key
	 *            The element name (e.g. <code>"PRIMEM"</code>).
	 * @return The next {@link Element} on the list, or {@code null} if no more
	 *         element is available.
	 */
	public Element pullOptionalElement(String key) {
		key = key.toUpperCase(Locale.ROOT);
		final Iterator<Object> iterator = list.iterator();
		while (iterator.hasNext()) {
			final Object object = iterator.next();
			if (object instanceof Element) {
				final Element element = (Element) object;
				if (element.list != null && element.keyword.equals(key)) {
					iterator.remove();
					return element;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the next element, or {@code null} if there is no more element.
	 * The element is <strong>not</strong> removed from the list.
	 *
	 * @return The next element, or {@code null} if there is no more elements.
	 */
	public Object peek() {
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * Close this element.
	 *
	 * @throws ParseException
	 *             If the list still contains some unprocessed elements.
	 */
	public void close() throws ParseException {
		if (list != null && !list.isEmpty()) {
			String msg = String.format(Locale.ROOT,
					"Parameter \"%s\" was not expected.", list.get(0));
			throw new ParseException(complete(msg), offset + keyword.length());
		}
	}

}