/***************************************************************************
 * JVerbnet v1.2.0
 * Copyright (c) 2012 Massachusetts Institute of Technology
 * 
 * JVerbnet is distributed under the terms of the Creative Commons 
 * Attribution 3.0 Unported License, which means it may be freely used for 
 * all purposes, as long as proper acknowledgment is made.  See the license 
 * file included with this distribution for more details.
 ****************************************************************************/

package edu.mit.jverbnet.data.semantics;

import static edu.mit.jverbnet.util.Checks.NotNullEmptyOrBlank;

import java.util.HashMap;
import java.util.Map;

import edu.mit.jverbnet.data.IVerbnetType;
import edu.mit.jverbnet.data.VerbnetTypes;

/**
 * Predicate types. The values in this enum correspond to
 * the elements of &lt;xsd:simpleType name="predType"&gt; in the Verbnet xsd
 * file.
 * 
 * @author Mark A. Finlayson
 * @version 1.2.0
 * @since JVerbnet 1.0.0
 */
public class PredicateType implements IVerbnetType {
    
	/** 
	 * The name of the xsd:simpleType entry that describes this verbnet type in the XSD file.
	 *
	 * @since JVerbnet 1.0.0
	 */
	public static final String XSD_TYPE_NAME = "predType";
	
	// final fields
	private final String id;

	/**
	 * Constructor that creates a new frame type.
	 * 
	 * @param id
	 *            The id of the value
	 * @since JVerbnet 1.0.0
	 * @throws NullPointerException
	 *             if the id is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the id is empty or all whitespace
	 */
	protected PredicateType(String id){
		this.id = NotNullEmptyOrBlank.check("id", id);
	}
	
	/* 
	 * (non-Javadoc) 
	 *
	 * @see edu.mit.jverbnet.data.IVerbnetType#getID()
	 */
	public String getID(){
		return id;
	}
	
	// id map
	private static final Map<String, PredicateType> idMap = new HashMap<String, PredicateType>();

	/**
	 * Returns the object corresponding to the specified xsd name. The id is
	 * matched to values without regard to case. If no value has the specified
	 * id, a new value is created with that id and returned.
	 * 
	 * If the {@link VerbnetTypes#isPrintingIdWarnings()} flag is set, the method
	 * will print a warning to standard out if there is a value with the
	 * specified id was created, or if the specified id is not exactly identical
	 * to the returned value's id (i.e., differs in case).
	 * 
	 * @param id
	 *            the id of the type value as found in the xsd file and in the
	 *            xml data files.
	 * @return the type corresponding to the specified xsd name, or
	 *         <code>null</code> if none
	 * @throws NullPointerException
	 *             if the id is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the id is empty or all whitespace
	 * @since JVerbnet 1.0.0
	 */
	public static PredicateType getById(String id){
		NotNullEmptyOrBlank.check("id", id);
		PredicateType result = idMap.get(id.toLowerCase());
		
		if(result == null){
			result = new PredicateType(id);
			idMap.put(id.toLowerCase(), result);
		}
		
		// print warnings
		if(!result.getID().equals(id))
			VerbnetTypes.printIdNormalizationWarning(PredicateType.class, id);
		
		return result;
	}
}
