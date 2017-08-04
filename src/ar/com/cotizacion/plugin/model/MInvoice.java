package ar.com.cotizacion.plugin.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.openXpertya.model.MConversionRate;
import org.openXpertya.model.MConversionType;
import org.openXpertya.model.MCurrency;
import org.openXpertya.model.MPriceList;
import org.openXpertya.model.PO;
import org.openXpertya.plugin.MPluginPO;
import org.openXpertya.plugin.MPluginStatusPO;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.DB;
import org.openXpertya.util.TimeUtil;

public class MInvoice extends MPluginPO {
	
	/** Logger */
    private static CLogger	s_log	= CLogger.getCLogger(MInvoice.class);
    
    Properties invoiceContext;
    String transaction;

	public MInvoice(PO po, Properties ctx, String trxName, String aPackage) {
		super(po, ctx, trxName, aPackage);
		// TODO Auto-generated constructor stub
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
					invoice.getC_Currency_ID(), invoice.getDateInvoiced(), invoice.getAD_Org_ID(),
					invoice.getCtx()) == null) {
				MConversionRate newcr = new MConversionRate(invoice.getCtx(), 0, invoice.get_TrxName());
				newcr.setAD_Org_ID(invoice.getAD_Org_ID());
				newcr.setC_Currency_ID(priceListCurrency);
				newcr.setC_Currency_ID_To(invoice.getC_Currency_ID());
				newcr.setValidFrom(TimeUtil.getDay(2000, 1, 1)); //desde el 2000
				newcr.setValidTo(TimeUtil.getDay(2040, 12, 30)); //hasta el 2040
				newcr.setDivideRate(invoice.getExchangeRate());
				newcr.setMultiplyRate(new BigDecimal(1F/newcr.getDivideRate().floatValue()));
				newcr.setC_ConversionType_ID(114);//ID=114 -> conversion Directa
				newcr.save();
			}/* else{
				/* Si la moneda ya existia, solo la actualizo 
				MConversionRate toUpdate = this.getConversionRate(priceListCurrency, invoice.getC_Currency_ID(), null, invoice.getAD_Client_ID(), invoice.getAD_Org_ID());
				toUpdate.setDivideRate(invoice.getExchangeRate());
				toUpdate.setMultiplyRate(new BigDecimal(1F/toUpdate.getDivideRate().floatValue()));
				toUpdate.save();
			}*/
		}

		return super.preBeforeSave(po, newRecord);
	}
	
	@Override
	public MPluginStatusPO postBeforeSave(PO po, boolean newRecord) {
		// TODO Auto-generated method stub
		return super.postBeforeSave(po, newRecord);
	}
	
}
