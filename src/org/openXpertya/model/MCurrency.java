/*
 * @(#)MCurrency.java   12.oct 2007  Versión 2.2
 *
 *    El contenido de este fichero está sujeto a la  Licencia Pública openXpertya versión 1.1 (LPO)
 * en tanto en cuanto forme parte íntegra del total del producto denominado:  openXpertya, solución 
 * empresarial global , y siempre según los términos de dicha licencia LPO.
 *    Una copia  íntegra de dicha  licencia está incluida con todas  las fuentes del producto.
 *    Partes del código son copyRight (c) 2002-2007 de Ingeniería Informática Integrada S.L., otras 
 * partes son  copyRight (c)  2003-2007 de  Consultoría y  Soporte en  Redes y  Tecnologías  de  la
 * Información S.L.,  otras partes son copyRight (c) 2005-2006 de Dataware Sistemas S.L., otras son
 * copyright (c) 2005-2006 de Indeos Consultoría S.L., otras son copyright (c) 2005-2006 de Disytel
 * Servicios Digitales S.A., y otras  partes son  adaptadas, ampliadas,  traducidas, revisadas  y/o 
 * mejoradas a partir de código original de  terceros, recogidos en el ADDENDUM  A, sección 3 (A.3)
 * de dicha licencia  LPO,  y si dicho código es extraido como parte del total del producto, estará
 * sujeto a su respectiva licencia original.  
 *    Más información en http://www.openxpertya.org/ayuda/Licencia.html
 */

package org.openXpertya.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import org.openXpertya.util.CCache;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;

/**
 *      Currency Model.
 *
 *  @author Comunidad de Desarrollo openXpertya
 *         *Basado en Codigo Original Modificado, Revisado y Optimizado de:
 *         * Jorg Janke
 *  @version $Id: MCurrency.java,v 1.7 2005/03/11 20:28:32 jjanke Exp $
 */
public class MCurrency extends X_C_Currency {

    /** Store System Currencies */
    private static CCache	s_currencies	= new CCache("C_Currency", 50);

    public MCurrency(Properties ctx, ResultSet rs, String trxName) {
    	super(ctx, rs, trxName);
    }
    
    /**
     *      Currency Constructor
     *      @param ctx context
     *      @param C_Currency_ID id
     * @param trxName
     */
    public MCurrency(Properties ctx, int C_Currency_ID, String trxName) {

        super(ctx, C_Currency_ID, trxName);

        if (C_Currency_ID == 0) {

            setIsEMUMember(false);
            setIsEuro(false);
            setStdPrecision(2);
            setCostingPrecision(4);
        }

    }		// MCurrency

    /**
     *      Currency Constructor
     *      @param ctx context
     *      @param ISO_Code ISO
     *      @param Description Name
     *      @param CurSymbol symbol
     *      @param StdPrecision prec
     *      @param CostingPrecision prec
     * @param trxName
     */
    public MCurrency(Properties ctx, String ISO_Code, String Description, String CurSymbol, int StdPrecision, int CostingPrecision, String trxName) {

        super(ctx, 0, trxName);
        setISO_Code(ISO_Code);
        setDescription(Description);
        setCurSymbol(CurSymbol);
        setStdPrecision(StdPrecision);
        setCostingPrecision(CostingPrecision);
        setIsEMUMember(false);
        setIsEuro(false);

    }		// MCurrency

    /**
     *      String Representation
     *      @return info
     */
    public String toString() {
        return "MCurrency[" + getC_Currency_ID() + "-" + getISO_Code() + "-" + getCurSymbol() + "," + getDescription() + ",Precision=" + getStdPrecision() + "/" + getCostingPrecision();
    }		// toString

    //~--- get methods --------------------------------------------------------

    /**
     *      Get Currency
     *      @param ctx Context
     *      @param C_Currency_ID currency
     *      @return ISO Code
     */
    public static MCurrency get(Properties ctx, int C_Currency_ID) {
    	return get(ctx, C_Currency_ID, null);
    }		// get

    
    /**
     *      Get Currency
     *      @param ctx Context
     *      @param C_Currency_ID currency
     *      @param trxName 
     *      @return ISO Code
     */
    public static MCurrency get(Properties ctx, int C_Currency_ID, String trxName) {

        // Try Cache
        Integer		key		= new Integer(C_Currency_ID);
        MCurrency	retValue	= (MCurrency) s_currencies.get(key);

        if (retValue != null) {
            return retValue;
        }

        // Create it
        retValue	= new MCurrency(ctx, C_Currency_ID, trxName);

        // Save in System
        if (retValue.getAD_Client_ID() == 0) {
            s_currencies.put(key, retValue);
        }

        return retValue;
    }		// get
    
    /**
     * @param ctx
     * @param ISO_Code
     * @return 
     */
    public static MCurrency get(Properties ctx, String ISO_Code) {
		return MCurrency.get(
				ctx,
				DB.getSQLValue(null, "SELECT c_currency_id FROM " + Table_Name
						+ " WHERE upper(trim(ISO_Code)) = upper(trim('"
						+ ISO_Code + "'))"));
    }
    
    /**
     *      Get Currency Iso Code.
     *      @param ctx Context
     *      @param C_Currency_ID currency
     *      @return ISO Code
     */
    public static String getISO_Code(Properties ctx, int C_Currency_ID) {

        String	contextKey	= "C_Currency_" + C_Currency_ID;
        String	retValue	= ctx.getProperty(contextKey);

        if (retValue != null) {
            return retValue;
        }

        // Create it
        MCurrency	c	= get(ctx, C_Currency_ID);

        retValue	= c.getISO_Code();
        ctx.setProperty(contextKey, retValue);

        return retValue;

    }		// getISO

    
    
    /**
     *      Get Standard Precision.
     *      @param ctx Context
     *      @param C_Currency_ID currency
     *      @return Standard Precision
     */
    public static int getStdPrecision(Properties ctx, int C_Currency_ID) {
    	return getStdPrecision(ctx, C_Currency_ID, null);
    }		// getStdPrecision
    
    /**
     *      Get Standard Precision.
     *      @param ctx Context
     *      @param C_Currency_ID currency
     *      @return Standard Precision
     */
    public static int getStdPrecision(Properties ctx, int C_Currency_ID, String trxName) {
        MCurrency	c	= get(ctx, C_Currency_ID, trxName);
        return c.getStdPrecision();
    }		// getStdPrecision

    
    public static BigDecimal currencyConvert(BigDecimal amount, int currencyFrom, int currencyTo, Date date, int adOrg, Properties ctx )
    {
    	BigDecimal result = null;
    	try
    	{
    		StringBuffer sql = new StringBuffer("SELECT currencyconvert (?, ?, ?, ? ::timestamp, null, ?, ");
    		if (adOrg > 0)
    			sql.append( "? )");
    		else
    			sql.append( "null )");

    		PreparedStatement pstmt = DB.prepareStatement(sql.toString());
    		pstmt.setBigDecimal(1, amount);
    		pstmt.setInt(2, currencyFrom);
    		pstmt.setInt(3, currencyTo);
    		//pstmt.setDate(4, new  java.sql.Date(date.getTime()) );
    		// currencyconvert requiere un timestamp como parametro. En ciertos casos 
    		// estaba funcionando mal con el Date. 
    		pstmt.setTimestamp(4, new Timestamp(date.getTime()) ); 
    		pstmt.setInt(5, Env.getAD_Client_ID(ctx) );
    		
    		if (adOrg > 0)
    			pstmt.setInt(6, adOrg );
    		
    		ResultSet rs = pstmt.executeQuery();
    		if (rs.next())
    			result = rs.getBigDecimal(1);
    	}
    	catch (Exception e ) {
    		e.printStackTrace();
    	}
		return result;
    }    
    
    /**
     * Realiza la conversion de moneda utilizando la cotizacion de la factura
     * Si amount == null || amount == 0 => convierte el total de la factura
     * Si foreign = true => convierte de moneda extranjera a  ARS
     * Si foreign = false => convierte de ARS a moneda extranjera 
     **/
    public static BigDecimal currencyConvertByInvoice(BigDecimal amount, int invoice_id, boolean foreign){
    	BigDecimal result = null;
    	try {
    		StringBuffer sql = new StringBuffer("SELECT convertByInvoice(?, ?, ?)");   

    		PreparedStatement pstmt = DB.prepareStatement(sql.toString());
    		pstmt.setBigDecimal(1, amount);
    		pstmt.setInt(2, invoice_id);
    		pstmt.setBoolean(3, foreign);
    		ResultSet rs = pstmt.executeQuery();
    		if (rs.next())
    			result = rs.getBigDecimal(1);
    	}
    	catch (Exception e ) {
    		e.printStackTrace();
    	}
		return result;
    }   
    
}