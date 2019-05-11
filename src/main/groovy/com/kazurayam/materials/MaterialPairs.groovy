package com.kazurayam.materials

import java.nio.file.Path

interface MaterialPairs {
    
    MaterialPair put(Path pathRelativeToTSuiteTimestamp, MaterialPair materialPair)
    
    MaterialPair putExpectedMaterial(Material expectedMaterial)
    
    MaterialPair putActualMaterial(Material actualMaterial)
    
    MaterialPair get(Path pathRelativeToTSuiteTimestamp)
    
    List<MaterialPair> getList()
    
    Set<Path> keySet()
    
    boolean containsKey(Path pathRelativeToTSuiteTimestamp)
    
    int size()
    
}
