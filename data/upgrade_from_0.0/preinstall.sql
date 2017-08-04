----------------------------------------------------------------------
---------- Nuevas Tablas y/o Vistas 
----------------------------------------------------------------------

----------------------------------------------------------------------
---------- Nuevas columnas en tablas y/o vistas 
----------------------------------------------------------------------

ALTER TABLE C_Invoice ADD COLUMN ExchangeRate numeric(6,2);
----------------------------------------------------------------------
---------- Modificación de tablas y/o vistas
----------------------------------------------------------------------

/*
 Realiza la conversion de moneda utilizando la cotizacion de la factura
 Si amount == null || amount == 0 => convierte el total de la factura
 Si flag = true => convierte de moneda extranjera a  ARS
 Si flag = false => convierte de ARS a moneda extranjera 
*/
CREATE OR REPLACE FUNCTION convertByInvoice(amount numeric, invoice_id numeric, flag boolean) 
RETURNS numeric AS $$
	
DECLARE
	total numeric(10,2);
	exchange numeric(10,2);

BEGIN
	IF (invoice_id IS NULL OR invoice_id = 0 OR flag IS NULL) THEN
		RETURN NULL;
	END IF;
	
	SELECT grandTotal, exchangeRate INTO total, exchange FROM c_invoice WHERE c_invoice_id = invoice_id; 
	
	IF NOT FOUND THEN
       	RAISE NOTICE 'Invoice no encontrada - %', c_invoice;
		RETURN NULL;
	END IF;
	
	-- Si no hay tasa => tasa = 1;
	IF (exchange IS NULL OR exchange = 0) THEN
		exchange := 1;
	END IF;
	-- Si la cantidad recibida como parametro es > 0 => convierto la cantidad recibida como parametro
	IF (amount IS NOT NULL AND amount > 0) THEN
		total := amount;
	END IF;
	
	-- Convierto de moneda extranjera a ARS
	IF (flag) THEN	
		RETURN total * exchange;
	ELSE 
	-- Convierto de pesos a extranjera
		RETURN total / exchange;
	END IF;	
END;

$$ LANGUAGE plpgsql;

/*
Calcula la suma total de pagos/cobros realizados/recibidos de una factura
Realiza el calculo traduciendo todos los valores a moneda ARS y 
devuelve el resultado en dicha moneda
*/
CREATE OR REPLACE FUNCTION calculateAllocatedAmtToArs(p_c_invoice_id integer)
RETURNS numeric AS $$

DECLARE
	v_PaidAmt NUMERIC := 0;
	v_Temp NUMERIC;
	ar RECORD;
	ars_Currency_ID numeric;

BEGIN
	SELECT c_currency_id INTO ars_Currency_ID FROM C_Currency WHERE iso_code = 'ARS';
	
	FOR ar IN 
		SELECT a.AD_Client_ID, a.AD_Org_ID, al.Amount, al.DiscountAmt, al.WriteOffAmt, a.C_Currency_ID, a.DateTrx , al.C_Invoice_Credit_ID
		FROM C_AllocationLine al
		INNER JOIN C_AllocationHdr a ON (al.C_AllocationHdr_ID=a.C_AllocationHdr_ID)
		WHERE (al.C_Invoice_ID = p_C_Invoice_ID OR al.C_Invoice_Credit_ID = p_C_Invoice_ID ) -- condicion no en Adempiere
          	AND a.IsActive='Y'
	LOOP
	    -- Agregado, para facturas como pago
		IF (p_C_Invoice_ID = ar.C_Invoice_Credit_ID) THEN
		   v_Temp := ar.Amount;
		ELSE
		   v_Temp := ar.Amount + ar.DisCountAmt + ar.WriteOffAmt;
		END IF;
		-- Se asume que este v_Temp es no negativo
		-- Si la linea de pago es en ARS no hay que convertir
		IF (ar.C_Currency_ID IS NULL OR ar.C_Currency_ID = ars_currency_id) THEN -- si es null se toma como que es ARS
			v_PaidAmt := v_PaidAmt + v_Temp;
		ELSE
			v_PaidAmt := v_PaidAmt + convertByInvoice(v_Temp, p_C_Invoice_ID, true); -- Convierto a  ARS antes de sumar
		END IF;
	END LOOP;
	RETURN	v_PaidAmt;
END;

$$ LANGUAGE plpgsql;


/*
Calcula el resto de una factura (total - cobros)
Si dicha factura tiene un plan de pago, lo aplica
*/
CREATE OR REPLACE FUNCTION calculateInvoiceOpenAmount(
    p_c_invoice_id integer,
    p_c_invoicepayschedule_id integer)
RETURNS numeric AS $$

DECLARE
	v_TotalOpenAmt  	NUMERIC := 0;
	v_PaidAmt  	        NUMERIC := 0;
	v_Remaining	        NUMERIC := 0;
   	v_Precision            	NUMERIC := 0;
   	v_Min            	NUMERIC := 0.01; -- en Adempiere inferido desde Currency
   	s			RECORD;
	
	ars_currency_id integer;              
	i_currency_id integer;
	
BEGIN
	--id de la moneda ARS
	SELECT c_currency_id INTO ars_currency_ID FROM C_Currency WHERE iso_code = 'ARS';
	--id de la moneda de la factura 
	SELECT c_currency_id INTO i_currency_ID FROM c_invoice WHERE c_invoice_id = p_c_invoice_id; 
	
	-- Convierto el grandTotal de la factura a ARS
	-- Si ya estaba en ARS, solo retornara el grandTotal
	SELECT convertByInvoice(null, p_c_invoice_id, true) as GrandTotal, 
		(SELECT StdPrecision FROM C_Currency C WHERE C.C_Currency_ID = I.C_Currency_ID) AS StdPrecision 
	INTO v_TotalOpenAmt, v_Precision
	FROM	C_Invoice I		--	NO se corrige por CM o SpliPayment; se usa directamente C_Inovoice y ningun multiplicador
	WHERE	I.C_Invoice_ID = p_C_Invoice_ID;

	IF NOT FOUND THEN
       	RAISE NOTICE 'Invoice no econtrada - %', p_C_Invoice_ID;
		RETURN NULL;
	END IF;
	
	-- Total de los pagos recibidos por esa factura. En ARS
	v_PaidAmt := calculateAllocatedAmtToArs(p_C_Invoice_ID);

    --  Do we have a Payment Schedule ?
    IF (p_C_InvoicePaySchedule_ID IS NOT NULL AND p_C_InvoicePaySchedule_ID > 0) THEN --   if not valid = lists invoice amount
        v_Remaining := v_PaidAmt;
        FOR s IN 
        	SELECT  ips.C_InvoicePaySchedule_ID, convertByInvoice(ips.DueAmt, p_c_invoice_id, true) as DueAmt
	        FROM    C_InvoicePaySchedule ips
	        INNER JOIN C_Invoice i on (ips.C_Invoice_ID = i.C_Invoice_ID)
		WHERE	ips.C_Invoice_ID = p_C_Invoice_ID
	        AND   ips.IsValid='Y'
        	ORDER BY ips.DueDate
        LOOP
            IF (s.C_InvoicePaySchedule_ID = p_C_InvoicePaySchedule_ID) THEN
                v_TotalOpenAmt := s.DueAmt - v_Remaining;
                IF (v_TotalOpenAmt < 0) THEN
                    v_TotalOpenAmt := 0; -- Pagado totalmente
                END IF;
				EXIT; -- se sale del loop, ya que ya se encontro
            ELSE -- calculate amount, which can be allocated to next schedule
                v_Remaining := v_Remaining - s.DueAmt;
                IF (v_Remaining < 0) THEN
                    v_Remaining := 0;
                END IF;
            END IF;
        END LOOP;
    ELSE
        v_TotalOpenAmt := v_TotalOpenAmt - v_PaidAmt;
    END IF;
--  RAISE NOTICE ''== Total='' || v_TotalOpenAmt;

	--	Ignore Rounding
	IF (v_TotalOpenAmt >= -v_Min AND v_TotalOpenAmt <= v_Min) THEN
		v_TotalOpenAmt := 0;
	END IF;

	--	Round to currency precision
	v_TotalOpenAmt := ROUND(COALESCE(v_TotalOpenAmt,0), v_Precision);
	
	-- Si la factura era en moneda extranjera vuelvo a realizar el cambio ARS -> EXTR
	IF(ars_currency_ID != i_currency_id) THEN --Si no esta facurado en ARS => convierto a la moneda correspondiente
		RETURN convertByInvoice(v_TotalOpenAmt, p_c_invoice_id, false);
	END IF;
	RETURN v_TotalOpenAmt;
END;

$$ LANGUAGE plpgsql;


