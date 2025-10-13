public annotation V1StartEvent on service remote function;

public annotation V1TimerEventConfig TimerEvent on service remote function;

public type V1TimerEventConfig record {|
    decimal interval?;
|};

public annotation V1CtxParam on parameter;

public annotation V1ClientParam on parameter;

public annotation V1InputParam on parameter;

public class V1Listener {
    public function init() {
    }

    public isolated function 'start() returns error? {
    }

    public isolated function gracefulStop() returns error? {
    }

    public isolated function immediateStop() returns error? {
    }

    public isolated function attach(V1Service serviceType, string[]|string? name = ()) returns error? {
    }

    public isolated function detach(V1Service serviceType) returns error? {
    }
}

public type V1Service service object {
};
