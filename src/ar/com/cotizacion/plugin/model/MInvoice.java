package ar.com.cotizacion.plugin.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.openXpertya.model.MConversionRate;
import org.openXpertya.model.MCurrency;
import org.openXpertya.model.MInvoiceLine;
import org.openXpertya.model.MPriceList;
import org.openXpertya.model.MProductPrice;
import org.openXpertya.model.PO;
import org.openXpertya.plugin.MPluginDocAction;
import org.openXpertya.plugin.MPluginPO;
import org.openXpertya.plugin.MPluginStatusDocAction;
import org.openXpertya.plugin.MPluginStatusPO;
import org.openXpertya.process.DocAction;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.DB;
import org.openXpertya.util.TimeUtil;

public class MInvoice extends MPluginDocAction {
	
	/** Logger */
    private static CLogger	s_log	= CLogger.getCLogger(MInvoice.class);
    
    Properties invoiceContext;
    String transaction;

	public MInvoice(PO po, Properties ctx, String trxName, String aPackage) {
		super(po, ctx, trxName, aPackage);
	}
	
	/**
     *      Get Currency Conversion Rate
     *  @param  CurFrom_ID  The C_Currency_ID FROM
     *  @param  CurTo_ID    The C_Currency_ID TO
     *  @param  ConvDate    The Conversion date - if null - use current date
     *  @param  ConversionType_ID Conversion rate type - if 0 - use Default
     *      @param  AD_Client_ID client
     *      @param  AD_Org_ID       organization
     *  @return currency Rate or null
     */
    private MConversionRate getConversionRate(int CurFrom_ID, int CurTo_ID, Timestamp ConvDate, int AD_Client_ID, int AD_Org_ID) {
        // Conversion Type
        int	C_ConversionType_ID	= 114; //Tipo Directa

        // Conversion Date
        if (ConvDate == null) {
            ConvDate	= new Timestamp(System.currentTimeMillis());
        }

        // Get Rate
        String	sql	= "(SELECT * " + "FROM C_Conversion_Rate "
        				  + " WHERE C_Currency_ID=?"		// #1
                          + " AND C_Currency_ID_To=?"			// #2
                          + " AND C_ConversionType_ID=?"		// #3
                          + " AND ? BETWEEN ValidFrom AND ValidTo"	// #4      TRUNC (?) ORA-00932: inconsistent datatypes: expected NUMBER got TIMESTAMP
                          + " AND AD_Client_ID IN (0,?)"	// #5
                          + " AND AD_Org_ID IN (0,?) "		// #6
                          + " ORDER BY AD_Client_ID DESC, AD_Org_ID DESC, ValidFrom DESC)";
        
        MConversionRate	retValue = null;
        PreparedStatement pstmt = null;

        try {
            pstmt = DB.prepareStatement(sql);
            pstmt.setInt(1, CurFrom_ID);
            pstmt.setInt(2, CurTo_ID);
            pstmt.setInt(3, C_ConversionType_ID);
            pstmt.setTimestamp(4, ConvDate);
            pstmt.setInt(5, AD_Client_ID);
            pstmt.setInt(6, AD_Org_ID);
            
            ResultSet rs = pstmt.executeQuery();

            if(rs.next()) {
                retValue = new MConversionRate(this.invoiceContext, rs, this.transaction);
            }

            rs.close();
            pstmt.close();
            pstmt = null;

        } catch (Exception e) {
            s_log.log(Level.SEVERE, "getRate", e);
        }

        try {
            if(pstmt != null) {
                pstmt.close();
            }
            pstmt = null;
        } catch (Exception e) {
            pstmt	= null;
        }
        return retValue;
    }	// getRate

	@Override
	public MPluginStatusPO preBeforeSave(PO po, boolean newRecord) {
		LP_C_Invoice invoice = (LP_C_Invoice)po;
		this.transaction = invoice.get_TrxName();
		this.invoiceContext = invoice.getCtx();
		
		/* Si la moneda de la factura es diferente a a del precio de lista
		 * y no hay una tasa de conversion entre esas monedas
		 * entonces creo la conversion
		 */
		int priceListCurrency = new MPriceList(invoice.getCtx(), invoice.getM_PriceList_ID(),null).getC_Currency_ID();
		if(priceListCurrency != invoice.getC_Currency_ID()) {
			if(MCurrency.currencyConvert(new BigDecimal(1), priceListCurrency,
					invoice.getC_Currency_ID(), invoice.getDateInvoiced(), 0,
					invoice.getCtx()) == null) {
				MConversionRate newcr = new MConversionRate(invoice.getCtx(), 0, invoice.get_TrxName());
				newcr.setAD_Org_ID(0);
				newcr.setC_Currency_ID(priceListCurrency);
				newcr.setC_Currency_ID_To(invoice.getC_Currency_ID());
				newcr.setValidFrom(TimeUtil.getDay(2000, 1, 1)); //desde el 2000
				newcr.setValidTo(TimeUtil.getDay(2040, 12, 30)); //hasta el 2040
				newcr.setDivideRate(invoice.getExchangeRate());
				newcr.setMultiplyRate(new BigDecimal(1F/newcr.getDivideRate().floatValue()));
				newcr.setC_ConversionType_ID(114);//ID=114 -> conversion Directa
				newcr.save();
			}
			
		}
		
		/* 
		 * Cuando se carga una factura de proveedor en una tarifa con moneda extranjera
		 * Creo una tasa de conversion si es que no existe, para evitar errores en los
		 * diferentes chequeos 
		 */
		int ars_currency_id = 0;
		try {
    		StringBuffer sql = new StringBuffer("SELECT C_Currency_ID FROM c_currency WHERE iso_code = 'ARS'");   
    		PreparedStatement pstmt = DB.prepareStatement(sql.toString());    	
    		ResultSet rs = pstmt.executeQuery();
    		if (rs.next())
    			ars_currency_id = rs.getBigDecimal(1).intValue();
    	}
    	catch (Exception e ) {
    		e.printStackTrace();
    	}
		if(priceListCurrency != ars_currency_id) {			
			if(MCurrency.currencyConvert(new BigDecimal(1), priceListCurrency,
					ars_currency_id, invoice.getDateInvoiced(), 0,
					invoice.getCtx()) == null) {
				MConversionRate newcr = new MConversionRate(invoice.getCtx(), 0, invoice.get_TrxName());
				newcr.setAD_Org_ID(0);
				newcr.setC_Currency_ID(priceListCurrency);
				newcr.setC_Currency_ID_To(ars_currency_id);
				newcr.setValidFrom(TimeUtil.getDay(2000, 1, 1)); //desde el 2000
				newcr.setValidTo(TimeUtil.getDay(2040, 12, 30)); //hasta el 2040
				newcr.setMultiplyRate(invoice.getExchangeRate());
				newcr.setDivideRate(new BigDecimal(1F/newcr.getDivideRate().floatValue()));
				newcr.setC_ConversionType_ID(114);//ID=114 -> conversion Directa
				newcr.save();
			}
		}

		return super.preBeforeSave(po, newRecord);
	}
	
	public MPluginStatusDocAction postCompleteIt(DocAction document) {
		LP_C_Invoice invoice = (LP_C_Invoice)document;
		
		MPriceList pl = new MPriceList(invoice.getCtx(), invoice.getM_PriceList_ID(), invoice.get_TrxName());
		if (pl.isActualizarPreciosConFacturaDeCompra()) {
			//Actualizo los precios de productos a partir del precio de la factura
			MInvoiceLine[] l = invoice.getLines();
			for (int i = 0; i < l.length; i++) {
				int plv_id = DB.getSQLValue(invoice.get_TrxName(),
								"SELECT M_PriceList_Version_ID FROM M_PriceList_Version WHERE M_PriceList_ID = "
										+ pl.getM_PriceList_ID());
				MProductPrice pp;
				if (plv_id != 0) {
					//Si existe MProductoPrice la instancio, sino creo una nueva
					pp = MProductPrice.get(invoice.getCtx(), plv_id, l[i].getM_Product_ID(), invoice.get_TrxName());
					if (pp == null) {
						pp = new MProductPrice(invoice.getCtx(), 0, invoice.get_TrxName());
						pp.setM_PriceList_Version_ID(plv_id);
						pp.setM_Product_ID(l[i].getM_Product_ID());
					}
					pp.setPriceList(l[i].getPriceActual());
					pp.setPriceStd(l[i].getPriceActual());
					pp.save();
				}
			}
		}
		return status_docAction;
	}
	
}
