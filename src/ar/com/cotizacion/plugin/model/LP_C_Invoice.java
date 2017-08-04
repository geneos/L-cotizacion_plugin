/** Modelo Generado - NO CAMBIAR MANUALMENTE - Disytel */
package ar.com.cotizacion.plugin.model;
import org.openXpertya.model.*;
import java.util.logging.Level;
 import java.util.*;
import java.sql.*;
import java.math.*;
import org.openXpertya.util.*;
/** Modelo Generado por C_Invoice
 *  @author Comunidad de Desarrollo Libertya*         *Basado en Codigo Original Modificado, Revisado y Optimizado de:*         * Jorg Janke 
 *  @version  - 2017-07-21 12:18:10.19 */
public class LP_C_Invoice extends org.openXpertya.model.MInvoice
{
/** Constructor estándar */
public LP_C_Invoice (Properties ctx, int C_Invoice_ID, String trxName)
{
super (ctx, C_Invoice_ID, trxName);
/** if (C_Invoice_ID == 0)
{
}
 */
}
/** Load Constructor */
public LP_C_Invoice (Properties ctx, ResultSet rs, String trxName)
{
super (ctx, rs, trxName);
}
public String toString()
{
StringBuffer sb = new StringBuffer ("LP_C_Invoice[").append(getID()).append("]");
return sb.toString();
}
/** Set ExchangeRate */
public void setExchangeRate (BigDecimal ExchangeRate)
{
set_Value ("ExchangeRate", ExchangeRate);
}
/** Get ExchangeRate */
public BigDecimal getExchangeRate() 
{
BigDecimal bd = (BigDecimal)get_Value("ExchangeRate");
if (bd == null) return Env.ZERO;
return bd;
}
}
