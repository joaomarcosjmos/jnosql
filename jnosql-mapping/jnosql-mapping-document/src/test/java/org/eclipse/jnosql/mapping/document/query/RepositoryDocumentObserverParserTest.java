/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.mapping.document.query;

import org.eclipse.jnosql.communication.document.DocumentObserverParser;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryDocumentObserverParserTest {

    @Test
    void shouldCreateFromRepository() {
        EntityMetadata entityMetadata = Mockito.mock(EntityMetadata.class);
        DocumentObserverParser parser = RepositoryDocumentObserverParser.of(entityMetadata);
        assertNotNull(parser);
    }
}