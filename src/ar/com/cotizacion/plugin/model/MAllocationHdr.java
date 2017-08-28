package ar.com.cotizacion.plugin.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.openXpertya.model.MAllocationLine;
import org.openXpertya.model.PO;
import org.openXpertya.plugin.MPluginDocAction;
import org.openXpertya.plugin.MPluginStatusDocAction;
import org.openXpertya.process.DocAction;
import org.openXpertya.util.DB;

public class MAllocationHdr extends MPluginDocAction {
	
	public MAllocationHdr(PO po, Properties ctx, String trxName, String aPackage) {
		super(po, ctx, trxName, aPackage);
	}
	
	public MPluginStatusDocAction postCompleteIt(DocAction document) {
		
		org.openXpertya.model.MAllocationHdr allhdr = (org.openXpertya.model.MAllocationHdr)document;
		
		/*
		 * Error 1
		 * El siguiente fragmento de codigo resuelve un error, que al hacer un pago parcial
		 * de una factura, la colocaba como paga, aunque no se haya pagado el total 
		 */
		String sql1 = "SELECT * FROM C_AllocationLine WHERE C_AllocationHdr_ID=?";
        
        PreparedStatement pstmt = null;

        try {
            pstmt = DB.prepareStatement( sql1, allhdr.get_TrxName());
            pstmt.setInt( 1, allhdr.getC_AllocationHdr_ID());
            ResultSet rs = pstmt.executeQuery();
            while( rs.next()) {
                MAllocationLine line = new MAllocationLine(allhdr.getCtx(), rs, allhdr.get_TrxName());
                int c_Invoice_ID = line.getC_Invoice_ID();
    			if (c_Invoice_ID != 0) {
    				String sql2 = " SELECT calculateInvoiceOpenAmount(C_Invoice_ID, 0) " +
    					         " FROM C_Invoice WHERE C_Invoice_ID=?";
    				BigDecimal open = DB.getSQLValueBD(allhdr.get_TrxName(), sql2, c_Invoice_ID);

    				if ((open != null) && (open.signum() > 0)) {
    					String sql3 = " UPDATE C_Invoice SET IsPaid='N' " +
    						  " WHERE C_Invoice_ID=" + c_Invoice_ID;

    					DB.executeUpdate(sql3, allhdr.get_TrxName());
    				}
    			}
            }

            rs.close();
            pstmt.close();
            pstmt = null;
        } catch( Exception e ) {
        	status_docAction.setContinueStatus(MPluginStatusDocAction.STATE_FALSE);
			status_docAction.setDocStatus(DocAction.STATUS_Invalid);
			status_docAction.setProcessMsg("No se pudieron extraer la cabeceras de las lineas de asignacion");
        }

        try {
            if( pstmt != null ) {
                pstmt.close();
            }
            pstmt = null;
        } catch( Exception e ) {
            pstmt = null;
        }
        /* Fin Error 1*/
              
        return status_docAction;
	}
	

}
