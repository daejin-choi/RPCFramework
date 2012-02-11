package rpc.framework.benchmark;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import rpc.framework.Callback;
import rpc.framework.NettyRpc;
import rpc.framework.ProtoParamRpcServer;
import rpc.framework.RemoteException;
import rpc.framework.test.BenchmarkProto.benchmarkRequest;
import rpc.framework.test.BenchmarkProto.benchmarkResponse;


public class BenchmarkAsyncRPC {

  public static class ClientWrapper extends Thread {
    public void run() {
      BenchmarkClientInterface service = null;
      service =
          (BenchmarkClientInterface) NettyRpc.getProtoParamAsyncRpcProxy(
              BenchmarkServerInterface.class, BenchmarkClientInterface.class,
              new InetSocketAddress(15010));

      long start = System.currentTimeMillis();
      Callback<benchmarkResponse> cb = new Callback<benchmarkResponse>();
      benchmarkRequest req = benchmarkRequest.newBuilder().setIn1(start).build();
      for (int i = 0; i < 10000; i++) {
        service.shoot(cb, req);
      }
      long end = System.currentTimeMillis();

      System.out.println("elapsed time: " + (end - start) + "msc");
    }
  }

  public static interface BenchmarkClientInterface {
    public void shoot(Callback<benchmarkResponse> ret, benchmarkRequest l) throws RemoteException;
  }

  public static interface BenchmarkServerInterface {
    public benchmarkResponse shoot(benchmarkRequest l) throws RemoteException;
  }

  public static class BenchmarkImpl implements BenchmarkServerInterface {
    @Override
    public benchmarkResponse shoot(benchmarkRequest l) {
      benchmarkResponse ret = benchmarkResponse.newBuilder().setRst(System.currentTimeMillis()).build();
      return ret;
    }
  }

  public static void main(String[] args) throws Exception {
    ProtoParamRpcServer rpcServer =
        NettyRpc.getProtoParamRpcServer(new BenchmarkImpl(),
            new InetSocketAddress(15010));
    rpcServer.start();
    Thread.sleep(1000);

    int numThreads = 10;
    ClientWrapper client[] = new ClientWrapper[numThreads];
    for (int i = 0; i < numThreads; i++) {
      client[i] = new ClientWrapper();
    }

    for (int i = 0; i < numThreads; i++) {
      client[i].start();
    }

    for (int i = 0; i < numThreads; i++) {
      client[i].join();
    }

    rpcServer.shutdown();
    System.exit(0);
  }
}
