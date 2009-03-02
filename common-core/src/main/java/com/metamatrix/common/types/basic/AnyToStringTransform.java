/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package com.metamatrix.common.types.basic;

import com.metamatrix.common.types.*;

/**
 * This class can be subclassed to do a simple anything-->String by
 * just calling toString().  Just extend and implement getSouceType().
 */
public abstract class AnyToStringTransform extends AbstractTransform {

	/**
	 * Type of the incoming value.
	 * @return Source type
	 */
	public abstract Class getSourceType();
	
	/**
	 * Type of the outgoing value.
	 * @return Target type
	 */
	public Class getTargetType() {
		return String.class;
	}

	/**
	 * This method transforms a value of the source type into a value
	 * of the target type.
	 * @param value Incoming value - Integer
	 * @return Outgoing value - String
	 * @throws TransformationException if value is an incorrect input type or
	 * the transformation fails
	 */
	public Object transform(Object value) throws TransformationException {
		if(value == null) {
			return null;
		}

		String result = value.toString();
		if (result != null && result.length() > DataTypeManager.MAX_STRING_LENGTH) {
			return result.substring(0, DataTypeManager.MAX_STRING_LENGTH);
		}
		return result;
	}
	
}
