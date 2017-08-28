package ar.com.cotizacion.plugin.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.openXpertya.util.DB;

public class InvoiceCurrencyConverter {

	public InvoiceCurrencyConverter() {}
	
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
