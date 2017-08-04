package ar.com.cotizacion.plugin.model;

import java.math.BigDecimal;
import java.util.Properties;

import org.openXpertya.model.PO;
import org.openXpertya.model.X_C_Payment;
import org.openXpertya.plugin.MPluginPO;
import org.openXpertya.plugin.MPluginStatusPO;

public class MPayment extends MPluginPO {

	public MPayment(PO po, Properties ctx, String trxName, String aPackage) {
		super(po, ctx, trxName, aPackage);
	}

	@Override
	public MPluginStatusPO preBeforeSave(PO po, boolean newRecord) {
		X_C_Payment payment = (X_C_Payment)po;
		LP_C_Invoice invoice = new LP_C_Invoice(payment.getCtx(), payment.getC_Invoice_ID(), payment.get_TrxName());		
		if(payment.getIsReceipt().equals("Y")) {
			/* Si la moneda de la factura y la del pago difieren, se debe hacer la conversion
			 * segun la cotizacion de la factura
			 */
			if(invoice.getC_Currency_ID() == payment.getC_Currency_ID()) {
				payment.setPayAmt(new BigDecimal(1));
			}
		}
		return super.preBeforeSave(po, newRecord);
	}	

}
