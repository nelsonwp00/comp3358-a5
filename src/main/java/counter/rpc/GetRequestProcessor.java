package counter.rpc;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.rpc.RpcContext;
import counter.Closure;
import counter.Service;
import counter.rpc.Outter.GetRequest;
import com.alipay.sofa.jraft.rpc.RpcProcessor;

public class GetRequestProcessor implements RpcProcessor<GetRequest> {
    private final Service service;

    public GetRequestProcessor(Service service) {
        super();
        this.service = service;
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final GetRequest request) {
        final Closure closure = new Closure() {
            @Override
            public void run(Status status) {
                rpcCtx.sendResponse(getValueResponse());
            }
        };

        this.service.get(closure);
    }

    @Override
    public String interest() {
        return GetRequest.class.getName();
    }
}
