/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  @author Victor Belov
 *  @since 1.4.0
 *
 */
package org.openhab.habdroid.util;


public class MyWebImage {
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;


    private String url;
    private boolean useCache = true;
    
    private String authUsername;
    private String authPassword;
    private boolean shouldAuth = false;

    public MyWebImage(String url) {
        this.url = url;
        this.useCache = true;
    }
    
    public MyWebImage(String url, String username, String password) {
        this.url = url;
        this.useCache = true;
        this.setAuthentication(username, password);
    }
    
    public MyWebImage(String url, boolean useCache) {
    	this.url = url;
    	this.useCache = useCache;
    }

    public MyWebImage(String url, boolean useCache, String username, String password) {
    	this.url = url;
    	this.useCache = useCache;
        this.setAuthentication(username, password);
    }

    public void setAuthentication(String username, String password) {
    	this.authUsername = username;
    	this.authPassword = password;
    	if (this.authUsername != null && (this.authUsername.length() > 0 && this.authPassword.length() > 0))
    		this.shouldAuth = true;
    }
}
