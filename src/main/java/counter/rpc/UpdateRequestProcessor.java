package counter.rpc;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import counter.Closure;
import counter.Service;
import counter.rpc.Outter.UpdateRequest;

public class UpdateRequestProcessor implements RpcProcessor<UpdateRequest> {
    private final Service service;

    public UpdateRequestProcessor(Service service) {
        super();
        this.service = service;
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final UpdateRequest request) {
        final Closure closure = new Closure() {
            @Override
            public void run(Status status) {
                rpcCtx.sendResponse(getValueResponse());
            }
        };

        this.service.update(request.getChange(), closure);
    }

    @Override
    public String interest() {
        return UpdateRequest.class.getName();
    }
}
