#pragma once

namespace test {
class Runner;
}

namespace modules_facade_pipeline_smoke {

bag::CoreConfig MakeConfig(bag::TransportMode mode);

void PushPcmInChunks(bag::ITransportDecoder* decoder,
                     const std::vector<std::int16_t>& pcm,
                     std::size_t chunk_size,
                     const std::string& message_prefix);
void PushPcmInChunks(bag::IPipeline* pipeline,
                     const std::vector<std::int16_t>& pcm,
                     std::size_t chunk_size,
                     const std::string& message_prefix);

void RegisterTransportFacadeSmokeTests(test::Runner& runner);
void RegisterPipelineSmokeTests(test::Runner& runner);

}  // namespace modules_facade_pipeline_smoke
