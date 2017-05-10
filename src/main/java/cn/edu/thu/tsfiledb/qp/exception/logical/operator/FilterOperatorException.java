package cn.edu.thu.tsfiledb.qp.exception.logical.operator;

import cn.edu.thu.tsfiledb.qp.exception.QueryProcessorException;

/**
 * This exception is threw whiling meeting error in
 * {@linkplain cn.edu.thu.tsfiledb.qp.logical.operator.crud.FilterOperator FilterOperator}
 * 
 * @author kangrong
 *
 */
public class FilterOperatorException extends QueryProcessorException {

    private static final long serialVersionUID = 167597682291449523L;

    public FilterOperatorException(String msg) {
        super(msg);
    }

}
