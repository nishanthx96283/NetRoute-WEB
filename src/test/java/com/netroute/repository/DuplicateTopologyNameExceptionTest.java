package com.netroute.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateTopologyNameExceptionTest {

    @Test
    void exceptionRetainsNameAndId() {
        DuplicateTopologyNameException ex = new DuplicateTopologyNameException("Campus Network", 42);
        assertEquals("Campus Network", ex.getTopologyName());
        assertEquals(42, ex.getExistingTopologyId());
        assertTrue(ex.getMessage().contains("Campus Network"));
        assertTrue(ex.getMessage().contains("42"));
    }
}
