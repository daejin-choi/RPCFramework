package rpc.framework;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rpc.framework.Callback;
import rpc.framework.NettyRpc;
import rpc.framework.ProtoParamRpcServer;
import rpc.framework.RemoteException;
import rpc.framework.test.DummyProtos.MulRequest1;
import rpc.framework.test.DummyProtos.MulRequest2;
import rpc.framework.test.DummyProtos.MulResponse;

import static org.junit.Assert.*;


public class TestProtoParamAsyncRpc {
  public static String MESSAGE = TestProtoParamAsyncRpc.class.getName();
  ProtoParamRpcServer server;
  DummyClientInterface proxy;

  public static interface DummyServerInterface {
    public Object throwException(MulRequest1 request) throws IOException;
    public MulResponse mul(MulRequest1 req1, MulRequest2 req2);
  }

  public static interface DummyClientInterface {
    public void throwException(Callback<Object> callback, MulRequest1 request)
        throws IOException;
    public void mul(Callback<MulResponse> callback, MulRequest1 req1, MulRequest2 req2);
  }

  public static class DummyServer implements DummyServerInterface {
    @Override
    public Object throwException(MulRequest1 request) throws IOException {
      throw new IOException();
    }

    @Override
    public MulResponse mul(MulRequest1 req1, MulRequest2 req2) {
      int x1_1 = req1.getX1();
      int x1_2 = req1.getX2();
      int x2_1 = req2.getX1();
      int x2_2 = req2.getX2();

      int result1 = x1_1 * x2_1;
      int result2 = x1_2 * x2_2;

      MulResponse rst = MulResponse.newBuilder().setResult1(result1).setResult2(result2).build();
      return rst;
    }

  }

  @Before
  public void setUp() throws Exception {
    server =
        NettyRpc.getProtoParamRpcServer(new DummyServer(),
            new InetSocketAddress(0));
    server.start();

    InetSocketAddress addr = server.getBindAddress();

    proxy =
        (DummyClientInterface) NettyRpc.getProtoParamAsyncRpcProxy(
            DummyServerInterface.class, DummyClientInterface.class, addr);
  }

  @After
  public void tearDown() throws IOException {
    server.shutdown();
    NettyRpc.shutdownAsyncProxy(proxy);
  }

  MulResponse answer1 = null;

  @Test
  public void testRpcRemoteException() throws Exception {

    MulRequest1 req = MulRequest1.newBuilder().setX1(10).setX2(20).build();

    System.out.println("Do whatever you want before get result!!");

    try {
      Callback<Object> cb = new Callback<Object>();
      proxy.throwException(cb, req);
      cb.get();

    } catch (RemoteException e) {
      System.out.println(e.getMessage());
    }

  }

  @Test
  public void testRpcProtoType() throws Exception {

    MulRequest1 req1 = MulRequest1.newBuilder().setX1(10).setX2(20).build();
    MulRequest2 req2 = MulRequest2.newBuilder().setX1(10).setX2(20).build();

    System.out.println("Do whatever you want before get result!!");

    try {
      Callback<MulResponse> cb = new Callback<MulResponse>();
      proxy.mul(cb, req1, req2);

      MulResponse resp = (MulResponse) cb.get();
      assertEquals(100, resp.getResult1());
      assertEquals(400, resp.getResult2());

    } catch (RemoteException e) {
      System.out.println(e.getMessage());
    }

  }

}
