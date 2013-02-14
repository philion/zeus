package com.acmerocket.zeus.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.lang3.StringUtils;

public interface Receiver {
    public Receiver pwrOn();
    public Receiver pwrOff();
    public Receiver setInput(String input);
    public Receiver volumeUp();
    public Receiver volumeDown();
    public Receiver setVolume(String value);
    public Receiver setName(String name);

    public String name();
    public String volume(); 
    public String input();
    
    public static final class Factory {
        public static Receiver wrap(Device device) {
            InvocationHandler handler = new ReceiverHandler(device);
            return (Receiver)Proxy.newProxyInstance(Receiver.class.getClassLoader(), 
                    new Class[] { Receiver.class }, handler);            
        }
        
        // FIXME REMOVE
//        public static void main(String[] args) throws Exception {
//            Device device = new TelnetDevice("192.168.1.21");
//            Receiver receiver = Receiver.Factory.wrap(device);
//            //System.out.println(receiver);
//            System.out.println(receiver.on().volumeUp().setInput("tivo").input());
//            //System.out.println(receiver.on());
//        }
    }
    
    public static final class ReceiverHandler implements InvocationHandler {
        private final Device delegate;
        private ReceiverHandler(Device device) { this.delegate = device; }
        @Override
        public Object invoke(Object proxy, Method method, Object[] objArgs) throws Throwable {
            //System.out.println(">> " + method.getName() + (objArgs != null ? Arrays.asList(objArgs) : "[]"));
            
            String command = method.getName();
            // strip 'set'
            if (command.startsWith("set")) {
                command = command.substring(3);
                command = StringUtils.uncapitalize(command);
            }
            
            // process capitol chars
            StringBuffer cmdBuffer = new StringBuffer();
            for (char ch : command.toCharArray()) {
                if (Character.isUpperCase(ch)) {
                    cmdBuffer.append('-').append(Character.toLowerCase(ch));
                }
                else {
                    cmdBuffer.append(ch);
                }
            }
            command = cmdBuffer.toString();
            
            // build args
            String[] args = null;
            if (objArgs != null && objArgs.length > 0) {
                args = new String[objArgs.length];
                for (int i = 0; i < objArgs.length; i++) {
                    args[i] = objArgs[i].toString();
                }
            }
            
            //System.out.println("<< " + command + (objArgs != null ? Arrays.asList(args) : "[]"));
            
            String response = this.delegate.sendCommand(command, args);
            
            // return either a string or a Receiver
            Class<?> rtnType = method.getReturnType();
            if (rtnType.isAssignableFrom(Receiver.class)) {
                return proxy;
            }
            else {
                return response;
            }            
        }
    }
}