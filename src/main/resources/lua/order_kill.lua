--获取优惠券id
local voucherId=AGV[1];
local userId=AGV[2];

--库存
local stock='seckill:stock:'..voucherId

local order='seckill:order:'..voucherId

if(tonumber(redis.call('get',stock))<=0) then
    return 1
end

if(redis.call('sismember',order,userId)==1) then
    return 2;
end

redis.call('incrby', stock ,-1 )
redis.call('sadd', order,userId)

return 0;