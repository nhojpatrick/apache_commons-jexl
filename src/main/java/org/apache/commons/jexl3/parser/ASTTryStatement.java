/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.parser;

/**
 * Declares a try/catch/finally statement.
 */
public class ASTTryStatement extends JexlLexicalNode {
    private static final long serialVersionUID = 1L;
    /** try {}=0; try(expr()) &=2; try(let x = expr()); &=1 . */
    /** catch() &= 4, finally &= 8. */
    private int tryForm;

    void setTryForm(final int form) {
        tryForm = form;
    }

    public int getTryForm() {
        return tryForm;
    }

    public ASTTryStatement(final int id) {
        super(id);
    }

    public ASTTryStatement(final Parser p, final int id) {
        super(p, id);
    }

    @Override
    public Object jjtAccept(final ParserVisitor visitor, final Object data) {
        return visitor.visit(this, data);
    }

}
