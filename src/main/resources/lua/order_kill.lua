--获取优惠券id
local voucherId= ARGV[1];
local userId= ARGV[2];

--库存
local stock_Key='seckill:stock:'..voucherId
redis.call('sadd','seckill:order',0)
local order_key='seckill:order'

if(tonumber(redis.call('get',stock_Key))<=0) then
    return 1 --库存不足
end
--判断是否下过单
--if(redis.call('get',order_key,userId)==nil) then

    if(redis.call('sismember',order_key,userId)==1) then
        return 2; -- 下过单了
    end

    redis.call('incrby', stock_Key ,-1 ) --库存减一
    redis.call('sadd', order_key,userId) --在order里加一
    return 0;--下单成功
--end
--return 2;--下过单了
