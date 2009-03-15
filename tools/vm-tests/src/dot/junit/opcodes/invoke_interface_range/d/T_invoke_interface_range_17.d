; Copyright (C) 2008 The Android Open Source Project
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;      http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

.source ITestImplProtected.java
.class public dot.junit.opcodes.invoke_interface_range.ITestImplProtected
.super java/lang/Object
.implements dot/junit/opcodes/invoke_interface_range/ITest

.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       return-void
.end method

.method protected test(I)I
.limit regs 2
    const v0, 0
    return v0
.end method




.source T_invoke_interface_range_17.java
.class public dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_17
.super java/lang/Object


.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       return-void
.end method


.method public run()V
.limit regs 7
       new-instance v0, Ldot/junit/opcodes/invoke_interface_range/ITestImplProtected;
       invoke-direct v0, dot.junit.opcodes.invoke_interface_range.ITestImplProtected/<init>()V
       
       const v1, 0
       invoke-interface/range {v0..v1}, dot/junit/opcodes/invoke_interface_range/ITest/test(I)I
       
       return-void
.end method


