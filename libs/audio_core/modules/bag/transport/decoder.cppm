export module bag.transport.decoder;

export import bag.common.error_code;
export import bag.common.types;

export namespace bag {

class ITransportDecoder {
public:
    virtual ~ITransportDecoder() = default;

    virtual ErrorCode PushPcm(const PcmBlock& block) = 0;
    virtual ErrorCode PollTextResult(TextResult* out_result) = 0;
    virtual void Reset() = 0;
};

}  // namespace bag
