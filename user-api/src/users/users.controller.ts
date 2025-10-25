import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  UseGuards,
  Request,
  HttpStatus,
  HttpCode,
} from '@nestjs/common';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { LoginUserDto } from './dto/login-user.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';
import { UserRole } from './schemas/user.schema';

@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Post('register')
  @HttpCode(HttpStatus.CREATED)
  async register(@Body() createUserDto: CreateUserDto) {
    const user = await this.usersService.create(createUserDto);
    const { password, refreshToken, ...result } = user.toObject();
    return result;
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  async login(@Body() loginUserDto: LoginUserDto) {
    // Login logic will be handled by AuthService
    return { message: 'Login endpoint - to be implemented with AuthService' };
  }

  @UseGuards(JwtAuthGuard)
  @Get('profile')
  getProfile(@Request() req) {
    return req.user;
  }

  @UseGuards(JwtAuthGuard)
  @Get(':id')
  async findOne(@Param('id') id: string) {
    const user = await this.usersService.findById(id);
    const { password, refreshToken, ...result } = user.toObject();
    return result;
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.ADMIN)
  @Get()
  async findAll(
    @Query('page') page = 0,
    @Query('size') size = 10,
    @Query('email') email?: string,
    @Query('role') role?: string,
    @Query('status') status?: string,
  ) {
    const filters = { email, role, status };
    const result = await this.usersService.findAll(page, size, filters);
    return {
      content: result.users.map(user => {
        const { password, refreshToken, ...userData } = user.toObject();
        return userData;
      }),
      totalElements: result.total,
      totalPages: Math.ceil(result.total / size),
      currentPage: page,
      size,
    };
  }

  @UseGuards(JwtAuthGuard)
  @Put(':id')
  async update(@Param('id') id: string, @Body() updateUserDto: UpdateUserDto, @Request() req) {
    // TODO: Add authorization check (user can only update their own profile or admin)
    const user = await this.usersService.update(id, updateUserDto);
    const { password, refreshToken, ...result } = user.toObject();
    return result;
  }

  @UseGuards(JwtAuthGuard)
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  async remove(@Param('id') id: string, @Request() req) {
    // TODO: Add authorization check (user can only delete their own account or admin)
    await this.usersService.remove(id);
  }
}