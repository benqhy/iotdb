package cn.edu.thu.tsfiledb.qp.logical.optimizer.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.thu.tsfiledb.qp.exception.logical.optimize.MergeFilterException;
import cn.edu.thu.tsfile.timeseries.read.qp.Path;
import cn.edu.thu.tsfiledb.qp.logical.operator.crud.FilterOperator;

public class MergeSingleFilterOptimizer implements IFilterOptimizer {
    Logger LOG = LoggerFactory.getLogger(MergeSingleFilterOptimizer.class);

    @Override
    public FilterOperator optimize(FilterOperator filter) {
        try {
            mergeSamePathFilter(filter);
        } catch (MergeFilterException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        return filter;
    }

    /**
     * 
     * merge and extract node with same Path recursively. <br>
     * If a node has more than two children and some children has same paths, remove them from this
     * node and merge them to a new single node, then add the new node to this children list.<br>
     * if all recursive children of this node have same path, set this node to single node, and
     * return the same path, otherwise, throw exception;
     * 
     * @param children - children is not empty.
     * 
     * @return - if all recursive children of this node have same path, set this node to single
     *         node, and return the same path, otherwise, throw exception;
     */
    public Path mergeSamePathFilter(FilterOperator src) throws MergeFilterException {
        if (src.isLeaf())
            return src.getSinglePath();
        List<FilterOperator> children = src.getChildren();
        if (children.isEmpty()) {
            throw new MergeFilterException("this inner filter has no children!");
        }
        if (children.size() == 1) {
            throw new MergeFilterException("this inner filter has just one child!");
        }
        Path childPath = mergeSamePathFilter(children.get(0));
        Path tempPath;
        for (int i = 1; i < children.size(); i++) {
            tempPath = mergeSamePathFilter(children.get(i));
            // if one of children differs with others or is not single node(path = null), src's path
            // is null
            if (tempPath != null && !tempPath.equals(childPath))
                childPath = null;
        }
        if (childPath != null) {
            src.setIsSingle(true);
            src.setSinglePath(childPath.clone());
            return childPath;
        }
        // extract same node
        Collections.sort(children);
        List<FilterOperator> ret = new ArrayList<FilterOperator>();

        List<FilterOperator> tempExtrNode = null;
        int i;
        for (i = 0; i < children.size(); i++) {
            tempPath = children.get(i).getSinglePath();
            // sorted by path, all "null" paths are at last
            if (tempPath == null) {
                // childPath = null;
                break;
            }
            if (childPath == null) {
                // first child to be add
                childPath = tempPath;
                tempExtrNode = new ArrayList<FilterOperator>();
                tempExtrNode.add(children.get(i));
            } else if (childPath.equals(tempPath)) {
                // successive next single child with same path,merge it with previous children
                tempExtrNode.add(children.get(i));
            } else {
                // not more same, add exist nodes in tempExtrNode into a new node
                // prevent make a node which has only one child.
                if (tempExtrNode.size() == 1) {
                    ret.add(tempExtrNode.get(0));
                    // use exist Object directly for efficiency
                    tempExtrNode.set(0, children.get(i));
                    childPath = tempPath;
                } else {
                    // add a new inner node
                    FilterOperator newFil = new FilterOperator(src.getTokenIntType(), true);
                    newFil.setSinglePath(childPath.clone());
                    newFil.setChildrenList(tempExtrNode);
                    ret.add(newFil);
                    tempExtrNode = new ArrayList<FilterOperator>();
                    tempExtrNode.add(children.get(i));
                    childPath = tempPath;
                }
            }
        }
        // the last several children before null has not been added to ret list.
        if (childPath != null) {
            if (tempExtrNode.size() == 1) {
                ret.add(tempExtrNode.get(0));
            } else {
                // add a new inner node
                FilterOperator newFil = new FilterOperator(src.getTokenIntType(), true);
                newFil.setSinglePath(childPath.clone());
                newFil.setChildrenList(tempExtrNode);
                ret.add(newFil);
            }
        }
        // add last null children
        for (; i < children.size(); i++) {
            ret.add(children.get(i));
        }
        if (ret.size() == 1) {
            // all children have same path, which means this src node is a single node
            src.setIsSingle(true);
            src.setSinglePath(childPath.clone());
            src.setChildrenList(ret.get(0).getChildren());
            return childPath;
        } else {
            src.setIsSingle(false);
            src.setChildrenList(ret);
            return null;
        }
    }
}
