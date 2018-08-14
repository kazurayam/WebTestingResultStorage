package com.kazurayam.material

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RepositoryVisitorSimpleImpl implements RepositoryVisitor {

    static Logger logger_ = LoggerFactory.getLogger(RepositoryVisitorSimpleImpl.class)

    /**
     * this PrintWriter instance will be reused by other RepositoryVisitor which extends this
     */
    protected PrintWriter pw_

    RepositoryVisitorSimpleImpl(Writer wr) {
        pw_ = new PrintWriter(new BufferedWriter(wr))
    }

    @Override
    RepositoryVisitResult preVisitRepositoryRoot(RepositoryRoot repoRoot) {
        pw_.println("#preVisitRepositoryRoot ${repoRoot.getBaseDir().toString()}")
        pw_.flush()
        return RepositoryVisitResult.SUCCESS
    }

    @Override
    RepositoryVisitResult postVisitRepositoryRoot(RepositoryRoot repoRoot) {
        pw_.println("#postVisitRepositoryRoot ${repoRoot.getBaseDir().toString()}")
        pw_.flush()
        return RepositoryVisitResult.SUCCESS
    }

    @Override
    RepositoryVisitResult preVisitTSuiteResult(TSuiteResult tSuiteResult) {
        pw_.println("#preVisitTSuiteResult ${tSuiteResult.treeviewTitle()}")
        pw_.flush()
        return RepositoryVisitResult.SUCCESS
    }

    @Override
    RepositoryVisitResult postVisitTSuiteResult(TSuiteResult tSuiteResult) {
        pw_.println("#postVisitTSuiteResult ${tSuiteResult.treeviewTitle()}")
        pw_.flush()
        return RepositoryVisitResult.SUCCESS
    }

    @Override
    RepositoryVisitResult preVisitTCaseResult(TCaseResult tCaseResult) {
        pw_.println("#preVisitTCaseResult ${tCaseResult.getTCaseName().getValue()}")
        pw_.flush()
        return RepositoryVisitResult.SUCCESS
    }

    @Override
    RepositoryVisitResult postVisitTCaseResult(TCaseResult tCaseResult) {
        pw_.println("#postVisitTCaseResult ${tCaseResult.getTCaseName().getValue()}")
        pw_.flush()
        return RepositoryVisitResult.SUCCESS
    }

    @Override
    RepositoryVisitResult visitMaterial(Material material) {
        pw_.println("#visitMaterial ${material.getIdentifier()}")
        pw_.flush()
        return RepositoryVisitResult.SUCCESS
    }

    @Override
    RepositoryVisitResult visitMaterialFailed(Material material, IOException ex) {
        pw_.println("#visitMaterialFailed ${material.getIdentifier()}")
        pw_.flush()
        return RepositoryVisitResult.FAILURE
    }

}
