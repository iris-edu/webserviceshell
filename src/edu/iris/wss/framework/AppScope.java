package edu.iris.wss.framework;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;

// This class configures the Application to know about the app-scoped class,
// SingletonWrapper classes through @Context annotations.

@Provider
public class AppScope extends SingletonTypeInjectableProvider<Context, SingletonWrapper> {

	public  AppScope() {
		super(SingletonWrapper.class, new SingletonWrapper());
	}
}