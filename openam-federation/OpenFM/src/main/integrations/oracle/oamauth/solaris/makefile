#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# https://opensso.dev.java.net/public/CDDLv1.0.html or
# opensso/legal/CDDLv1.0.txt
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at opensso/legal/CDDLv1.0.txt.
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#

#
# Makefile for the sample Oracle NetPoint Authentication Plugin
#
# Please customize to your environment
#
# Note: this makefile expects to be in 
#      /INSTALLDIR/sdk/authentication/samples/general/
#

CC = /tools/ns/bin/gcc-2.95.2
RM = rm -f
AUTHN_INCLUDE_DIR =include

OBJS = authn_api.o
DSO  = authn_api.so

CC_FLAGS = -I$(AUTHN_INCLUDE_DIR) -c -KPIC

GCC_FLAGS = -I$(AUTHN_INCLUDE_DIR) -c -fPIC -Wall 
LIBS =  -lamsdk -lxml2 \
	-L /usr/lib/mps -lssl3 -lnss3 -lplc4 -lplds4 -lnspr4 \
	-L /usr/ucblib -lucb

CFLAGS=     $(GCC_FLAGS)

LDFLAGS = -L lib $(LIBS)

$(DSO): $(OBJS) 
	$(CC) -G $(OBJS) $(LDFLAGS) -o $(DSO)

$(OBJS): authn_api.c
	$(CC) $(CFLAGS) $(LDFLAGS) -o $(OBJS) authn_api.c


clean:
	$(RM) $(OBJS) $(DSO)
